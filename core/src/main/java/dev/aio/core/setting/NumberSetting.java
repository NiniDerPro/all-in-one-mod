package dev.aio.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class NumberSetting extends Setting<Double> {
    private final double min;
    private final double max;
    private final double step;
    private final String suffix;

    public NumberSetting(String id, String name, double defaultValue, double min, double max, double step) {
        this(id, name, defaultValue, min, max, step, "");
    }

    public NumberSetting(String id, String name, double defaultValue, double min, double max, double step, String suffix) {
        super(id, name, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step;
        this.suffix = suffix;
    }

    public double min() { return min; }

    public double max() { return max; }

    public double step() { return step; }

    public float floatValue() { return value.floatValue(); }

    public int intValue() { return (int) Math.round(value); }

    /** Setzt den Wert über einen Anteil 0..1, z. B. von einem Slider. */
    public void setFraction(double fraction) {
        set(min + (max - min) * Math.max(0, Math.min(1, fraction)));
    }

    public double fraction() { return max == min ? 0 : (value - min) / (max - min); }

    public String display() {
        double v = value;
        String s = step >= 1 ? String.valueOf((long) Math.round(v)) : String.format(java.util.Locale.ROOT, step >= 0.1 ? "%.1f" : "%.2f", v);
        return s + suffix;
    }

    @Override protected Double sanitize(Double v) {
        if (v == null || v.isNaN() || v.isInfinite()) return null;
        double clamped = Math.max(min, Math.min(max, v));
        if (step > 0) clamped = min + Math.round((clamped - min) / step) * step;
        // Rundungsfehler bei Kommaschritten glätten
        clamped = Math.round(clamped * 1e6) / 1e6;
        return Math.max(min, Math.min(max, clamped));
    }

    @Override public JsonElement toJson() { return new JsonPrimitive(value); }

    @Override public void fromJson(JsonElement json) {
        if (json != null && json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) set(json.getAsDouble());
    }
}
