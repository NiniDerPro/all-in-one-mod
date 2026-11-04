package dev.aio.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import dev.aio.core.module.Module;
import dev.aio.core.module.ModuleRegistry;
import dev.aio.core.setting.Setting;
import dev.aio.core.util.Log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.LongSupplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * JSON-Config pro Modul, organisiert in Profilen.
 *
 * <pre>
 * &lt;configDir&gt;/client.json                     aktives Profil
 * &lt;configDir&gt;/profiles/&lt;profil&gt;/&lt;modul&gt;.json   eine Datei pro Modul
 * &lt;configDir&gt;/exports/&lt;profil&gt;.json           exportierte Profile
 * </pre>
 *
 * Änderungen werden gesammelt und frühestens {@link #SAVE_DELAY_MS} nach der letzten Änderung
 * geschrieben, damit ein Slider-Zug nicht hunderte Schreibvorgänge auslöst.
 */
public final class ConfigManager {
    public static final String DEFAULT_PROFILE = "default";
    public static final long SAVE_DELAY_MS = 1000;
    public static final int EXPORT_FORMAT = 1;
    private static final Pattern PROFILE_NAME = Pattern.compile("[A-Za-z0-9_\\-]{1,32}");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path root;
    private final ModuleRegistry modules;
    private final LongSupplier clock;

    private String activeProfile = DEFAULT_PROFILE;
    private boolean dirty;
    private long lastChange;
    private boolean loading;

    public ConfigManager(Path root, ModuleRegistry modules, LongSupplier clock) {
        this.root = root;
        this.modules = modules;
        this.clock = clock;
    }

    public static boolean isValidProfileName(String name) {
        return name != null && PROFILE_NAME.matcher(name).matches();
    }

    public String activeProfile() { return activeProfile; }

    // ---- Laden / Speichern ----

    public void load() {
        JsonObject client = readObject(root.resolve("client.json"));
        if (client != null && client.has("activeProfile")) {
            String p = client.get("activeProfile").getAsString();
            if (isValidProfileName(p)) activeProfile = p;
        }
        loadProfile(activeProfile);
    }

    private void loadProfile(String profile) {
        loading = true;
        try {
            Path dir = profileDir(profile);
            for (Module m : modules.all()) {
                applyModule(m, readObject(dir.resolve(m.id() + ".json")));
            }
        } finally {
            loading = false;
        }
        dirty = false;
    }

    public void markDirty() {
        if (loading) return;
        dirty = true;
        lastChange = clock.getAsLong();
    }

    public boolean isDirty() { return dirty; }

    /** Im Tick aufrufen: schreibt, wenn Änderungen lange genug zurückliegen. */
    public void tick() {
        if (dirty && clock.getAsLong() - lastChange >= SAVE_DELAY_MS) save();
    }

    /** Schreibt sofort (z. B. beim Beenden oder Profilwechsel). */
    public void save() {
        dirty = false;
        try {
            Path dir = profileDir(activeProfile);
            Files.createDirectories(dir);
            for (Module m : modules.all()) writeAtomic(dir.resolve(m.id() + ".json"), GSON.toJson(serializeModule(m)));
            JsonObject client = new JsonObject();
            client.addProperty("activeProfile", activeProfile);
            writeAtomic(root.resolve("client.json"), GSON.toJson(client));
        } catch (IOException e) {
            Log.error("Config konnte nicht gespeichert werden", e);
        }
    }

    // ---- Profile ----

    public List<String> profiles() {
        List<String> result = new ArrayList<>();
        Path dir = root.resolve("profiles");
        if (Files.isDirectory(dir)) {
            try (Stream<Path> s = Files.list(dir)) {
                s.filter(Files::isDirectory)
                        .map(p -> p.getFileName().toString())
                        .filter(ConfigManager::isValidProfileName)
                        .forEach(result::add);
            } catch (IOException e) {
                Log.error("Profile konnten nicht gelesen werden", e);
            }
        }
        if (!result.contains(activeProfile)) result.add(activeProfile);
        result.sort(Comparator.naturalOrder());
        return result;
    }

    public void switchProfile(String name) {
        requireValid(name);
        if (name.equals(activeProfile)) return;
        save();
        activeProfile = name;
        loadProfile(name);
        save();
    }

    /** Legt ein neues Profil als Kopie des aktuellen an und wechselt hinein. */
    public String createProfile(String baseName) {
        String name = uniqueName(baseName);
        save();
        activeProfile = name;
        save();
        return name;
    }

    public boolean deleteProfile(String name) {
        requireValid(name);
        if (name.equals(activeProfile)) return false;
        Path dir = profileDir(name);
        try (Stream<Path> s = Files.walk(dir)) {
            for (Path p : s.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p);
            return true;
        } catch (IOException e) {
            Log.error("Profil " + name + " konnte nicht gelöscht werden", e);
            return false;
        }
    }

    /** Exportiert das aktive Profil als ein JSON-Dokument (und zusätzlich nach exports/). */
    public String exportProfile() {
        JsonObject doc = new JsonObject();
        doc.addProperty("aioProfile", EXPORT_FORMAT);
        doc.addProperty("name", activeProfile);
        JsonObject mods = new JsonObject();
        for (Module m : modules.all()) mods.add(m.id(), serializeModule(m));
        doc.add("modules", mods);
        String json = GSON.toJson(doc);
        try {
            Path dir = root.resolve("exports");
            Files.createDirectories(dir);
            writeAtomic(dir.resolve(activeProfile + ".json"), json);
        } catch (IOException e) {
            Log.error("Export-Datei konnte nicht geschrieben werden", e);
        }
        return json;
    }

    /**
     * Importiert ein exportiertes Profil als neues Profil und wechselt hinein.
     *
     * @return Name des neuen Profils
     * @throws IllegalArgumentException bei ungültigem Inhalt
     */
    public String importProfile(String json) {
        JsonObject doc;
        try {
            JsonElement el = JsonParser.parseString(json == null ? "" : json.trim());
            if (!el.isJsonObject()) throw new IllegalArgumentException("Kein Profil (kein JSON-Objekt)");
            doc = el.getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            throw new IllegalArgumentException("Kein gültiges JSON");
        }
        if (!doc.has("aioProfile") || !doc.has("modules") || !doc.get("modules").isJsonObject()) {
            throw new IllegalArgumentException("Kein AIO-Profil");
        }
        String base = doc.has("name") && doc.get("name").isJsonPrimitive() ? doc.get("name").getAsString() : "import";
        base = base.replaceAll("[^A-Za-z0-9_\\-]", "");
        if (base.isEmpty()) base = "import";
        if (base.length() > 24) base = base.substring(0, 24);
        String name = uniqueName(base);

        save();
        activeProfile = name;
        JsonObject mods = doc.getAsJsonObject("modules");
        loading = true;
        try {
            for (Module m : modules.all()) {
                JsonElement el = mods.get(m.id());
                applyModule(m, el != null && el.isJsonObject() ? el.getAsJsonObject() : null);
            }
        } finally {
            loading = false;
        }
        save();
        return name;
    }

    // ---- Serialisierung ----

    static JsonObject serializeModule(Module m) {
        JsonObject obj = new JsonObject();
        obj.addProperty("enabled", m.isEnabled());
        JsonObject settings = new JsonObject();
        for (Setting<?> s : m.settings()) settings.add(s.id(), s.toJson());
        obj.add("settings", settings);
        return obj;
    }

    /** Setzt das Modul auf Standard zurück und übernimmt dann alle gültigen Werte aus {@code obj}. */
    static void applyModule(Module m, JsonObject obj) {
        m.resetToDefaults();
        if (obj == null) return;
        try {
            if (obj.has("settings") && obj.get("settings").isJsonObject()) {
                JsonObject settings = obj.getAsJsonObject("settings");
                for (Setting<?> s : m.settings()) {
                    if (settings.has(s.id())) s.fromJson(settings.get(s.id()));
                }
            }
            if (obj.has("enabled") && obj.get("enabled").isJsonPrimitive()) m.setEnabled(obj.get("enabled").getAsBoolean());
        } catch (RuntimeException e) {
            Log.error("Ungültige Config für Modul " + m.id() + ", nutze Standardwerte", e);
        }
    }

    // ---- Hilfen ----

    private Path profileDir(String profile) { return root.resolve("profiles").resolve(profile); }

    private String uniqueName(String base) {
        if (!isValidProfileName(base)) base = "profil";
        List<String> existing = profiles();
        if (!existing.contains(base)) return base;
        for (int i = 2; ; i++) {
            String candidate = base + "-" + i;
            if (!existing.contains(candidate)) return candidate;
        }
    }

    private static void requireValid(String name) {
        if (!isValidProfileName(name)) throw new IllegalArgumentException("Ungültiger Profilname: " + name);
    }

    private static JsonObject readObject(Path file) {
        if (!Files.isRegularFile(file)) return null;
        try {
            JsonElement el = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            return el.isJsonObject() ? el.getAsJsonObject() : null;
        } catch (IOException | JsonParseException e) {
            Log.error("Config-Datei " + file + " ist beschädigt, nutze Standardwerte", e);
            return null;
        }
    }

    private static void writeAtomic(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(tmp, content, StandardCharsets.UTF_8);
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
