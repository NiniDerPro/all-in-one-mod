package dev.aio.core.i18n;

import java.util.IllegalFormatException;
import java.util.Locale;
import java.util.function.Function;

/**
 * Translated texts. Keys live in {@code assets/aio/lang/<language>.json}; the English text given
 * at the call site is the fallback when a key is missing (and the source for en_us.json).
 */
public final class Lang {
    private Lang() {}

    private static volatile Function<String, String> lookup = key -> null;

    /** Set by the platform adapter: returns the translation or null. */
    public static void setLookup(Function<String, String> source) {
        lookup = source == null ? key -> null : source;
    }

    public static String tr(String key, String fallback) {
        String value = lookup.apply(key);
        return value != null ? value : fallback;
    }

    /** Like {@link #tr(String, String)} with {@link String#format} arguments ({@code %s}, {@code %d}). */
    public static String tr(String key, String fallback, Object... args) {
        String pattern = tr(key, fallback);
        try {
            return String.format(Locale.ROOT, pattern, args);
        } catch (IllegalFormatException e) {
            return String.format(Locale.ROOT, fallback, args);
        }
    }
}
