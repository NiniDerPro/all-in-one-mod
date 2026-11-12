package dev.aio.fabric;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import dev.aio.core.AioCore;
import dev.aio.core.i18n.UiLanguage;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Language forced in the client settings. With "Auto" the texts follow the game language (Minecraft loads
 * {@code assets/aio/lang/<code>.json} itself); with a chosen language the file is read here, independent of
 * the game language. Missing keys fall back to the English text at the call site.
 */
final class LangSwitch {
    private LangSwitch() {}

    private static final Gson GSON = new Gson();
    private static final Map<String, Map<String, String>> CACHE = new HashMap<>();

    private static UiLanguage selected() {
        AioCore core = AioCore.get();
        return core == null || core.clientSettings() == null ? UiLanguage.AUTO : core.clientSettings().language.get();
    }

    /** Whether a language other than "Auto" is chosen. */
    static boolean forced() { return selected() != UiLanguage.AUTO; }

    /** Text of the chosen language or null (then the English fallback applies). */
    static String get(String key) {
        UiLanguage lang = selected();
        if (lang == UiLanguage.AUTO || lang == UiLanguage.ENGLISH) return null;
        return load(lang.code()).get(key);
    }

    private static synchronized Map<String, String> load(String code) {
        return CACHE.computeIfAbsent(code, c -> {
            try (var in = LangSwitch.class.getResourceAsStream("/assets/aio/lang/" + c + ".json")) {
                if (in == null) return Map.of();
                Map<String, String> m = GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8),
                        new TypeToken<Map<String, String>>() {}.getType());
                return m == null ? Map.<String, String>of() : m;
            } catch (Exception e) {
                return Map.of();
            }
        });
    }
}
