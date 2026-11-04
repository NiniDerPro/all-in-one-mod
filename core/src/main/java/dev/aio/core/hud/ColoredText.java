package dev.aio.core.hud;

import dev.aio.core.platform.Renderer;
import dev.aio.core.util.Colors;

/** Zeichnet Text in fester Farbe, als Regenbogen-Welle oder als wandernden Verlauf. */
public final class ColoredText {
    private ColoredText() {}

    /** Breite je Zeichen: MC-Schrift hat kein Kerning, also Text = Summe der Zeichen. */
    private static final java.util.Map<Integer, Integer> CHAR_WIDTH = new java.util.HashMap<>();
    private static int probeWidth = -1;

    private static int charWidth(Renderer r, int cp) {
        Integer w = CHAR_WIDTH.get(cp);
        if (w == null) {
            w = r.textWidth(new String(Character.toChars(cp)));
            CHAR_WIDTH.put(cp, w);
        }
        return w;
    }

    /** Schriftwechsel (z. B. Unicode-Schrift) erkennen: dann alle Breiten neu messen. */
    private static int drawsSinceProbe = Integer.MAX_VALUE;

    private static void checkFont(Renderer r) {
        if (++drawsSinceProbe < 512 && probeWidth != -1) return; // selten prüfen reicht
        drawsSinceProbe = 0;
        int probe = r.textWidth("W");
        if (probe != probeWidth) {
            CHAR_WIDTH.clear();
            probeWidth = probe;
        }
    }

    /** Leert den Breiten-Cache, z. B. nach einem Wechsel der Schriftart. */
    public static void clearCache() {
        CHAR_WIDTH.clear();
        probeWidth = -1;
    }

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
        checkFont(r);
        // Ohne Cache pro Text: Koordinaten & Co. ändern sich ständig und würden ihn nur füllen
        int n = text.codePointCount(0, text.length());
        // Eigenes Array je Aufruf: Minecraft wertet den Text erst später beim Zeichnen aus
        int[] colors = new int[n];
        float[] offsets = new float[n];
        float cx = x;
        for (int i = 0, idx = 0; i < text.length(); idx++) {
            int cp = text.codePointAt(i);
            offsets[idx] = cx - x;
            if (mode == ColorMode.CHROMA) {
                colors[idx] = Colors.chroma(screenOffset + cx, speed, alpha);
            } else {
                float phase = (float) (0.5 + 0.5 * Math.sin((t * Math.PI * 2) - (screenOffset + cx) / 40.0));
                colors[idx] = Colors.lerp(primary, secondary, phase);
            }
            cx += charWidth(r, cp);
            i += Character.charCount(cp);
        }
        // Ein Aufruf für die ganze Zeile statt einem pro Zeichen (spart viel Arbeit pro Frame)
        r.coloredText(text, colors, offsets, x, y, shadow);
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
