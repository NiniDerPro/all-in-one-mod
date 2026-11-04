package dev.aio.core.hud;

import dev.aio.core.platform.Renderer;
import dev.aio.core.util.Colors;

/** Zeichnet Text in fester Farbe, als Regenbogen-Welle oder als wandernden Verlauf. */
public final class ColoredText {
    private ColoredText() {}

    /** Zerlegter Text: einzelne Zeichen und ihre x-Versätze. Wird pro Text gecacht, nicht pro Frame gemessen. */
    private record Glyphs(String[] chars, float[] offsets) {}

    private static final int CACHE_LIMIT = 256;
    /** LRU: wenig benutzte Texte fliegen einzeln raus, statt den ganzen Cache zu leeren. */
    private static final java.util.Map<String, Glyphs> CACHE = new java.util.LinkedHashMap<>(64, 0.75f, true) {
        @Override protected boolean removeEldestEntry(java.util.Map.Entry<String, Glyphs> eldest) { return size() > CACHE_LIMIT; }
    };

    private static Glyphs glyphs(Renderer r, String text) {
        Glyphs g = CACHE.get(text);
        if (g != null) return g;
        int n = text.codePointCount(0, text.length());
        String[] chars = new String[n];
        float[] offsets = new float[n];
        float x = 0;
        for (int i = 0, idx = 0; i < text.length(); idx++) {
            int cp = text.codePointAt(i);
            chars[idx] = new String(Character.toChars(cp));
            offsets[idx] = x;
            x += r.textWidth(chars[idx]);
            i += Character.charCount(cp);
        }
        g = new Glyphs(chars, offsets);
        CACHE.put(text, g);
        return g;
    }

    /** Leert den Glyphen-Cache, z. B. nach einem Wechsel der Schriftart. */
    public static void clearCache() { CACHE.clear(); }

    /**
     * @param screenOffset Versatz für die Welle, damit Elemente nebeneinander nicht gleichfarbig pulsieren
     */
    public static void draw(Renderer r, String text, float x, float y, ColorMode mode, int primary, int secondary,
                            float speed, boolean shadow, float screenOffset) {
        if (text.isEmpty()) return;
        if (mode == ColorMode.STATIC) {
            r.text(text, x, y, primary, shadow);
            return;
        }
        int alpha = Colors.alpha(primary);
        double t = System.nanoTime() / 1e9 * speed;
        Glyphs g = glyphs(r, text);
        for (int i = 0; i < g.chars.length; i++) {
            float cx = x + g.offsets[i];
            int color;
            if (mode == ColorMode.CHROMA) {
                color = Colors.chroma(screenOffset + cx, speed, alpha);
            } else {
                float phase = (float) (0.5 + 0.5 * Math.sin((t * Math.PI * 2) - (screenOffset + cx) / 40.0));
                color = Colors.lerp(primary, secondary, phase);
            }
            r.text(g.chars[i], cx, y, color, shadow);
        }
    }

    /** Einzelne dynamische Farbe (z. B. für Flächen) passend zum Modus. */
    public static int color(ColorMode mode, int primary, int secondary, float speed, float offset) {
        return switch (mode) {
            case STATIC -> primary;
            case CHROMA -> Colors.chroma(offset, speed, Colors.alpha(primary));
            case GRADIENT -> {
                double t = System.nanoTime() / 1e9 * speed;
                yield Colors.lerp(primary, secondary, (float) (0.5 + 0.5 * Math.sin(t * Math.PI * 2 - offset / 40.0)));
            }
        };
    }
}
