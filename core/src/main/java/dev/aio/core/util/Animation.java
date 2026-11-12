package dev.aio.core.util;

/**
 * Time-based transition from 0 to 1 and back (frame-rate independent).
 * Used e.g. for the smooth colouring of the keystrokes.
 */
public final class Animation {
    private float value;
    private long lastNanos = -1;

    /**
     * @param target     0 or 1
     * @param durationMs duration for a complete transition; 0 = immediate
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
