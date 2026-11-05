package dev.aio.core.hud;

import dev.aio.core.platform.Renderer;
import dev.aio.core.util.Colors;

import java.util.HashMap;
import java.util.Map;

/** Zeichnet Text in fester Farbe, als Regenbogen-Welle oder als wandernden Verlauf. */
public final class ColoredText {
    private ColoredText() {}

    /** Breite je Zeichen: MC-Schrift hat kein Kerning, also Text = Summe der Zeichen. */
    private static final Map<Integer, Integer> CHAR_WIDTH = new HashMap<>();
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
    private static long lastProbe;

    private static void checkFont(Renderer r) {
        long now = System.nanoTime();
        if (probeWidth != -1 && now - lastProbe < 2_000_000_000L) return; // alle 2 s prüfen reicht
        lastProbe = now;
        int probe = r.textWidth("W");
        if (probe != probeWidth) {
            CHAR_WIDTH.clear();
            probeWidth = probe;
        }
    }

    /** Leert den Breiten-Cache, z. B. nach einem Wechsel der Schriftart. */
    public static void clearCache() {
        CHAR_WIDTH.clear();
        SEGMENTS.clear();
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
        if (mode == ColorMode.CLEAN) {
            drawClean(r, text, x, y, primary, shadow);
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

    /** Gedimmte Variante einer Textfarbe für Beschriftungen. */
    public static int dim(int primary) {
        return (primary & 0xFF000000) | (Colors.lerp(primary, 0xFF000000, 0.42f) & 0xFFFFFF);
    }

    /** Zerlegung eines Textes in Abschnitte (Wert/Beschriftung) mit x-Versatz, zwischengespeichert. */
    private record Segments(String[] parts, float[] offsets, boolean[] values) {}

    private static final java.util.Map<String, Segments> SEGMENTS = new java.util.LinkedHashMap<>(64, 0.75f, true) {
        @Override protected boolean removeEldestEntry(java.util.Map.Entry<String, Segments> e) { return size() > 256; }
    };

    /** Werte hell, Beschriftungen gedimmt; in Abschnitten gleicher Farbe gezeichnet. */
    private static void drawClean(Renderer r, String text, float x, float y, int primary, boolean shadow) {
        checkFont(r);
        Segments seg = SEGMENTS.get(text);
        if (seg == null) {
            java.util.List<String> parts = new java.util.ArrayList<>();
            java.util.List<Boolean> values = new java.util.ArrayList<>();
            int start = 0;
            boolean value = isValueChar(text, 0);
            for (int i = 1; i <= text.length(); i++) {
                boolean v = i < text.length() && isValueChar(text, i);
                if (i == text.length() || v != value) {
                    parts.add(text.substring(start, i));
                    values.add(value);
                    start = i;
                    value = v;
                }
            }
            float[] offsets = new float[parts.size()];
            boolean[] vals = new boolean[parts.size()];
            float cx = 0;
            for (int i = 0; i < parts.size(); i++) {
                offsets[i] = cx;
                vals[i] = values.get(i);
                cx += r.textWidth(parts.get(i));
            }
            seg = new Segments(parts.toArray(new String[0]), offsets, vals);
            SEGMENTS.put(text, seg);
        }
        int label = dim(primary);
        for (int i = 0; i < seg.parts.length; i++) {
            r.text(seg.parts[i], x + seg.offsets[i], y, seg.values[i] ? primary : label, shadow);
        }
    }

    /** Ziffern und was zu Zahlen gehört (Vorzeichen, Komma, Doppelpunkt in Uhrzeiten, %). */
    private static boolean isValueChar(String s, int i) {
        char c = s.charAt(i);
        if (Character.isDigit(c)) return true;
        boolean digitBefore = i > 0 && Character.isDigit(s.charAt(i - 1));
        boolean digitAfter = i + 1 < s.length() && Character.isDigit(s.charAt(i + 1));
        return switch (c) {
            case '.', ',', ':' -> digitBefore && digitAfter;
            case '-', '+' -> digitAfter;
            case '%', '°' -> digitBefore;
            default -> false;
        };
    }

    /** Einzelne dynamische Farbe (z. B. für Flächen) passend zum Modus. */
    public static int color(ColorMode mode, int primary, int secondary, float speed, float offset) {
        return switch (mode) {
            case STATIC, CLEAN -> primary;
            case CHROMA -> Colors.chroma(offset, speed, Colors.alpha(primary));
            case GRADIENT -> {
                double t = System.nanoTime() / 1e9 * speed;
                yield Colors.lerp(primary, secondary, (float) (0.5 + 0.5 * Math.sin(t * Math.PI * 2 - offset / 40.0)));
            }
        };
    }
}
