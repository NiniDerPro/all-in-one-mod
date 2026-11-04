package dev.aio.core.util;

public final class Colors {
    private Colors() {}

    public static int argb(int a, int r, int g, int b) {
        return (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
    }

    public static int alpha(int c) { return c >>> 24; }

    public static int withAlpha(int c, int alpha) { return (c & 0x00FFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24); }

    /** Multipliziert den Alphakanal, z. B. für Einblend-Animationen. */
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

    /** Grün bis Rot je nach Anteil (1 = grün). */
    public static int health(float fraction) {
        fraction = Math.max(0f, Math.min(1f, fraction));
        return lerp(0xFFFF5555, 0xFF55FF55, fraction);
    }
}
