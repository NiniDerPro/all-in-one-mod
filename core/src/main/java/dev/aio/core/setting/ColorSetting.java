package dev.aio.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** ARGB-Farbe. Wird als Hex-String {@code #AARRGGBB} gespeichert. */
public class ColorSetting extends Setting<Integer> {
    public ColorSetting(String id, String name, int defaultArgb) {
        super(id, name, defaultArgb);
    }

    public int argb() { return value; }

    public int channel(int shift) { return (value >>> shift) & 0xFF; }

    public void setChannel(int shift, int v) {
        int clamped = Math.max(0, Math.min(255, v));
        set((value & ~(0xFF << shift)) | (clamped << shift));
    }

    @Override public JsonElement toJson() { return new JsonPrimitive(String.format("#%08X", value)); }

    @Override public void fromJson(JsonElement json) {
        if (json == null || !json.isJsonPrimitive()) return;
        String s = json.getAsString().trim();
        if (s.startsWith("#")) s = s.substring(1);
        try {
            if (s.length() == 6) s = "FF" + s;
            if (s.length() == 8) set((int) Long.parseLong(s, 16));
        } catch (NumberFormatException ignored) {
            // ungültige Farbe: Wert behalten
        }
    }
}
