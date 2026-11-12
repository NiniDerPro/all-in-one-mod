package dev.aio.core.util;

public final class Colors {
    private Colors() {}

    public static int argb(int a, int r, int g, int b) {
        return (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
    }

    public static int alpha(int c) { return c >>> 24; }

    public static int withAlpha(int c, int alpha) { return (c & 0x00FFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24); }

    /** Multiplies the alpha channel, e.g. for fade-in animations. */
    public static int fade(int c, float factor) {
        return withAlpha(c, Math.round(alpha(c) * Math.max(0f, Math.min(1f, factor))));
    }

    /** Lineare Interpolation pro Kanal, t in 0..1. */
    public static int lerp(int from, int to, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = Math.round(alpha(from) + (alpha(to) - alpha(from)) * t);
        int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return argb(a, r, g, b);
    }

    /** HSV to ARGB. h, s, v in 0..1 (h is treated cyclically). */
    public static int hsv(float h, float s, float v, int alpha) {
        h = h - (float) Math.floor(h);
        float c = v * s;
        float x = c * (1 - Math.abs((h * 6) % 2 - 1));
        float m = v - c;
        float r, g, b;
        switch ((int) (h * 6) % 6) {
            case 0 -> { r = c; g = x; b = 0; }
            case 1 -> { r = x; g = c; b = 0; }
            case 2 -> { r = 0; g = c; b = x; }
            case 3 -> { r = 0; g = x; b = c; }
            case 4 -> { r = x; g = 0; b = c; }
            default -> { r = c; g = 0; b = x; }
        }
        return argb(alpha, Math.round((r + m) * 255), Math.round((g + m) * 255), Math.round((b + m) * 255));
    }

    /**
     * Rainbow colour that moves over time.
     *
     * @param offset offset in screen pixels (for the wave across the text)
     * @param speed  cycles per second
     */
    public static int chroma(float offset, float speed, int alpha) {
        double t = System.nanoTime() / 1e9 * speed;
        return hsv((float) (t - offset / 220.0), 0.75f, 1f, alpha);
    }

    /** Green to red depending on the fraction (1 = green). */
    public static int health(float fraction) {
        fraction = Math.max(0f, Math.min(1f, fraction));
        return lerp(0xFFFF5555, 0xFF55FF55, fraction);
    }
}
