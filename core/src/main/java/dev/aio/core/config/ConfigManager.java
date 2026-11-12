package dev.aio.core.config;

import dev.aio.core.hud.HudElement;
import dev.aio.core.module.Module;
import dev.aio.core.module.ModuleRegistry;
import dev.aio.core.setting.Setting;
import dev.aio.core.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

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
 * &lt;configDir&gt;/profiles/&lt;profile&gt;/&lt;module&gt;.json   one file per module
 * &lt;configDir&gt;/exports/&lt;profil&gt;.json           exportierte Profile
 * </pre>
 *
 * Changes are collected and written at the earliest {@link #SAVE_DELAY_MS} after the last change,
 * so that dragging a slider does not trigger hundreds of writes.
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

    /** Base folder of the config (e.g. for data that does not belong to a profile). */
    public Path root() { return root; }

    /** Reads a text file below {@link #root()}, null if it is missing. */
    public String readText(String fileName) {
        Path f = root.resolve(fileName);
        try {
            return Files.isRegularFile(f) ? Files.readString(f, StandardCharsets.UTF_8) : null;
        } catch (IOException e) {
            Log.error("File " + f + " could not be read", e);
            return null;
        }
    }

    /** Writes a text file below {@link #root()} atomically. */
    public void writeText(String fileName, String content) {
        try {
            writeAtomic(root.resolve(fileName), content);
        } catch (IOException e) {
            Log.error("File " + fileName + " could not be written", e);
        }
    }

    // ---- Laden / Speichern ----

    /**
     * Version of the default modules. When it rises, all modules are set once to
     * their (new) default on the next start; afterwards the user's own settings apply again.
     */
    public static final int DEFAULTS_VERSION = 2;

    public void load() {
        JsonObject client = readObject(root.resolve("client.json"));
        if (client != null && client.has("activeProfile")) {
            String p = client.get("activeProfile").getAsString();
            if (isValidProfileName(p)) activeProfile = p;
        }
        loadProfile(activeProfile);
        int version = client != null && client.has("defaultsVersion") ? client.get("defaultsVersion").getAsInt() : 0;
        if (client != null && version < DEFAULTS_VERSION) {
            for (Module m : modules.all()) {
                m.setEnabled(m.enabledByDefault());
                m.applyNewDefaults();
            }
            save();
        }
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

    /** Call every tick: writes when changes are old enough. */
    public void tick() {
        if (dirty && clock.getAsLong() - lastChange >= SAVE_DELAY_MS) save();
    }

    /** Writes immediately (e.g. on exit or profile switch). */
    public void save() {
        dirty = false;
        try {
            Path dir = profileDir(activeProfile);
            Files.createDirectories(dir);
            for (Module m : modules.all()) writeAtomic(dir.resolve(m.id() + ".json"), GSON.toJson(serializeModule(m)));
            JsonObject client = new JsonObject();
            client.addProperty("activeProfile", activeProfile);
            client.addProperty("defaultsVersion", DEFAULTS_VERSION);
            writeAtomic(root.resolve("client.json"), GSON.toJson(client));
        } catch (IOException e) {
            Log.error("Config could not be saved", e);
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
                Log.error("Profiles could not be read", e);
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

    /** Creates a new profile as a copy of the current one and switches to it. */
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
            Log.error("Profile " + name + " could not be deleted", e);
            return false;
        }
    }

    /** Exports the active profile as one JSON document (and additionally to exports/). */
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
            Log.error("Export file could not be written", e);
        }
        return json;
    }

    /**
     * Imports an exported profile as a new profile and switches to it.
     *
     * @return Name des neuen Profils
     * @throws IllegalArgumentException on invalid content
     */
    public String importProfile(String json) {
        JsonObject doc;
        try {
            JsonElement el = JsonParser.parseString(json == null ? "" : json.trim());
            if (!el.isJsonObject()) throw new IllegalArgumentException("Not a profile (no JSON object)");
            doc = el.getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            throw new IllegalArgumentException("Not valid JSON");
        }
        if (!doc.has("aioProfile") || !doc.has("modules") || !doc.get("modules").isJsonObject()) {
            throw new IllegalArgumentException("Not an AIO profile");
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

    /**
     * Applies a saved module config: first the settings (default + saved values),
     * then exactly once the on/off state. This way a module that the config says is off is never
     * activated in between (which would e.g. run code at game start that is not allowed to yet).
     */
    static void applyModule(Module m, JsonObject obj) {
        m.resetSettings();
        boolean enabled = m.enabledByDefault();
        if (obj != null) {
            try {
                if (obj.has("settings") && obj.get("settings").isJsonObject()) {
                    JsonObject settings = obj.getAsJsonObject("settings");
                    for (Setting<?> s : m.settings()) {
                        if (settings.has(s.id())) s.fromJson(settings.get(s.id()));
                    }
                    // Old configs only knew one shared size ("scale"): take it over for the height
                    if (m instanceof HudElement el && settings.has("scale") && !settings.has("scaleY")) {
                        el.setting("scaleY").fromJson(settings.get("scale"));
                    }
                    // Configs from before auto placement: do not move existing positions
                    if (m instanceof HudElement el && settings.has("x") && !settings.has("placed")) {
                        el.markPlaced();
                    }
                }
                if (obj.has("enabled") && obj.get("enabled").isJsonPrimitive()) enabled = obj.get("enabled").getAsBoolean();
            } catch (RuntimeException e) {
                Log.error("Invalid config for module " + m.id() + ", using defaults", e);
                m.resetSettings();
                enabled = m.enabledByDefault();
            }
        }
        m.afterLoad();
        m.setEnabled(m.adjustLoadedEnabled(enabled));
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
        if (!isValidProfileName(name)) throw new IllegalArgumentException("Invalid profile name: " + name);
    }

    private static JsonObject readObject(Path file) {
        if (!Files.isRegularFile(file)) return null;
        try {
            JsonElement el = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            return el.isJsonObject() ? el.getAsJsonObject() : null;
        } catch (IOException | JsonParseException e) {
            Log.error("Config file " + file + " is damaged, using defaults", e);
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
