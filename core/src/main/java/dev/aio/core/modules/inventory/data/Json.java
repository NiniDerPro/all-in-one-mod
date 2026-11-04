package dev.aio.core.modules.inventory.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/** Kleine Helfer für die Minecraft-Datenformate (Zahlen-Provider, IDs, Listen). */
final class Json {
    private Json() {}

    /** Wertebereich eines Zahlen-Providers. */
    record Range(double min, double max, double avg) {
        static final Range ONE = new Range(1, 1, 1);

        boolean isSingle() { return min == max; }

        String text() {
            return isSingle() ? fmt(min) : fmt(min) + "–" + fmt(max);
        }

        private static String fmt(double v) { return v == Math.rint(v) ? String.valueOf((long) v) : String.format(java.util.Locale.ROOT, "%.1f", v); }
    }

    /**
     * Zahl oder Provider: {@code 3}, {@code {"type":"uniform","min":1,"max":3}},
     * {@code {"min_inclusive":..,"max_inclusive":..}}, {@code {"type":"binomial","n":3,"p":0.5}}.
     */
    static Range range(JsonElement e, Range fallback) {
        if (e == null || e.isJsonNull()) return fallback;
        if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) {
            double v = e.getAsDouble();
            return new Range(v, v, v);
        }
        if (!e.isJsonObject()) return fallback;
        JsonObject o = e.getAsJsonObject();
        String type = str(o, "type");
        if (type.endsWith("binomial")) {
            double n = range(o.get("n"), Range.ONE).avg(), p = range(o.get("p"), new Range(0.5, 0.5, 0.5)).avg();
            return new Range(0, n, n * p);
        }
        if (type.endsWith("constant") || o.has("value")) return range(o.get("value"), fallback);
        JsonElement lo = o.has("min") ? o.get("min") : o.get("min_inclusive");
        JsonElement hi = o.has("max") ? o.get("max") : o.get("max_inclusive");
        if (lo == null && hi == null) return fallback;
        double a = range(lo, new Range(0, 0, 0)).min(), b = range(hi, new Range(a, a, a)).max();
        return new Range(a, b, (a + b) / 2);
    }

    static String str(JsonObject o, String key) {
        JsonElement e = o == null ? null : o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : "";
    }

    static JsonObject obj(JsonElement e) { return e != null && e.isJsonObject() ? e.getAsJsonObject() : null; }

    static JsonObject obj(JsonObject o, String key) { return o == null ? null : obj(o.get(key)); }

    static List<JsonElement> list(JsonObject o, String key) {
        JsonElement e = o == null ? null : o.get(key);
        List<JsonElement> out = new ArrayList<>();
        if (e instanceof JsonArray a) a.forEach(out::add);
        else if (e != null && !e.isJsonNull()) out.add(e);
        return out;
    }

    /** "iron_ore" → "minecraft:iron_ore". */
    static String id(String s) {
        if (s == null || s.isEmpty()) return "";
        if (s.startsWith("#")) return "#" + id(s.substring(1));
        return s.indexOf(':') < 0 ? "minecraft:" + s : s;
    }

    /** "minecraft:chests/desert_pyramid" → "desert_pyramid" (letzter Pfadteil). */
    static String lastPath(String id) {
        int i = Math.max(id.lastIndexOf('/'), id.lastIndexOf(':'));
        return id.substring(i + 1);
    }

    /** "ancient_city_ice_box" → "Ancient City Ice Box". */
    static String pretty(String path) {
        StringBuilder sb = new StringBuilder();
        for (String w : lastPath(path).split("_")) {
            if (w.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return sb.toString();
    }
}
