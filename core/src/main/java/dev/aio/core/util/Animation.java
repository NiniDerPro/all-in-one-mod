package dev.aio.core.util;

/**
 * Zeitbasierter Übergang von 0 nach 1 und zurück (framerate-unabhängig).
 * Wird z. B. für das sanfte Einfärben der Keystrokes genutzt.
 */
public final class Animation {
    private float value;
    private long lastNanos = -1;

    /**
     * @param target     0 oder 1
     * @param durationMs Dauer für einen kompletten Übergang; 0 = sofort
     * @return aktueller Wert in 0..1
     */
    public float update(float target, float durationMs, long nowNanos) {
        if (lastNanos < 0 || durationMs <= 0) {
            value = target;
        } else {
            float step = (nowNanos - lastNanos) / 1_000_000f / durationMs;
            if (value < target) value = Math.min(target, value + step);
            else if (value > target) value = Math.max(target, value - step);
        }
        lastNanos = nowNanos;
        return value;
    }

    public float value() { return value; }
}
