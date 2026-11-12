package dev.aio.core;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.setting.EnumSetting;
import dev.aio.core.setting.Setting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * en_us.json must contain every text: module names, descriptions, settings, enum values,
 * categories and all {@code Lang.tr("key", "fallback")} calls in the sources.
 * Regenerate with {@code -Daio.writeLang=true}.
 */
class LangFileTest {
    private static final Path FILE = Path.of("../fabric-26.3/src/main/resources/assets/aio/lang/en_us.json");
    private static final Pattern CALL = Pattern.compile("Lang\\.tr\\(\"([a-z0-9_.]+)\",\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    @TempDir Path dir;

    @Test void englishLanguageFileIsComplete() throws IOException {
        Map<String, String> expected = collect();
        String json = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(toJson(expected)) + "\n";
        if (Boolean.getBoolean("aio.writeLang")) {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, json, StandardCharsets.UTF_8);
            return;
        }
        assertTrue(Files.exists(FILE), "en_us.json fehlt: mit -Daio.writeLang=true erzeugen");
        JsonObject actual = JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8)).getAsJsonObject();
        for (Map.Entry<String, String> e : expected.entrySet()) {
            assertTrue(actual.has(e.getKey()), "Missing in en_us.json: " + e.getKey() + " (regenerate with -Daio.writeLang=true)");
            assertEquals(e.getValue(), actual.get(e.getKey()).getAsString(), "Veraltet in en_us.json: " + e.getKey());
        }
        for (String key : actual.keySet()) assertTrue(expected.containsKey(key), "Nicht mehr benutzt: " + key);
    }

    private Map<String, String> collect() throws IOException {
        Map<String, String> out = new TreeMap<>();
        AioCore core = new AioCore(new FakePlatform(), dir);
        core.registerDefaultModules();
        core.start();
        for (Category c : Category.values()) put(out, c.key(), c.defaultName());
        for (Module m : core.modules().all()) {
            put(out, "aio." + m.id(), m.defaultName());
            put(out, "aio." + m.id() + ".desc", m.defaultDescription());
            for (Setting<?> s : m.settings()) {
                if (s.isHidden() || s.translationKey() == null) continue;
                put(out, s.translationKey(), s.defaultName());
                if (s instanceof EnumSetting<?> es) {
                    for (Enum<?> v : es.values()) {
                        put(out, EnumSetting.key(v), v instanceof EnumSetting.Named n ? n.displayName() : v.name());
                    }
                }
            }
        }
        for (Path root : new Path[]{Path.of("src/main/java"), Path.of("../fabric-26.3/src/main/java")}) {
            if (!Files.isDirectory(root)) continue;
            try (Stream<Path> files = Files.walk(root)) {
                for (Path f : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".java"))::iterator) {
                    Matcher m = CALL.matcher(Files.readString(f, StandardCharsets.UTF_8));
                    while (m.find()) put(out, m.group(1), unescape(m.group(2)));
                }
            }
        }
        return out;
    }

    private static void put(Map<String, String> map, String key, String value) {
        String old = map.putIfAbsent(key, value);
        assertTrue(old == null || old.equals(value), "Key " + key + " with two texts: \"" + old + "\" / \"" + value + "\"");
    }

    private static String unescape(String s) {
        return s.replace("\\\"", "\"").replace("\\n", "\n").replace("\\\\", "\\");
    }

    private static JsonObject toJson(Map<String, String> map) {
        JsonObject o = new JsonObject();
        map.forEach(o::addProperty);
        return o;
    }
}
