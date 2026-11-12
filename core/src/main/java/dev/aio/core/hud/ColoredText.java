package dev.aio.core.hud;

import dev.aio.core.platform.Renderer;
import dev.aio.core.util.Colors;

import java.util.HashMap;
import java.util.Map;

/** Draws text in a fixed colour, as a rainbow wave or as a moving gradient. */
public final class ColoredText {
    private ColoredText() {}

    /** Width per character: the MC font has no kerning, so text = sum of the characters. */
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

    /** Detect a font change (e.g. unicode font): then re-measure all widths. */
    private static long lastProbe;

    private static void checkFont(Renderer r) {
        long now = System.nanoTime();
        if (probeWidth != -1 && now - lastProbe < 2_000_000_000L) return; // checking every 2 s is enough
        lastProbe = now;
        int probe = r.textWidth("W");
        if (probe != probeWidth) {
            CHAR_WIDTH.clear();
            probeWidth = probe;
        }
    }

    /** Clears the width cache, e.g. after a font change. */
    public static void clearCache() {
        CHAR_WIDTH.clear();
        SEGMENTS.clear();
        probeWidth = -1;
    }

    /**
     * @param screenOffset offset for the wave, so that elements next to each other do not pulse in the same colour
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
        // No per-text cache: coordinates & co. change constantly and would only fill it
        int n = text.codePointCount(0, text.length());
        // Own array per call: Minecraft only evaluates the text later when drawing
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
        // One call for the whole line instead of one per character (saves a lot of work per frame)
        r.coloredText(text, colors, offsets, x, y, shadow);
    }

    /** Dimmed variant of a text colour for labels. */
    public static int dim(int primary) {
        return (primary & 0xFF000000) | (Colors.lerp(primary, 0xFF000000, 0.42f) & 0xFFFFFF);
    }

    /** Splitting of a text into sections (value/label) with x offset, cached. */
    private record Segments(String[] parts, float[] offsets, boolean[] values) {}

    private static final java.util.Map<String, Segments> SEGMENTS = new java.util.LinkedHashMap<>(64, 0.75f, true) {
        @Override protected boolean removeEldestEntry(java.util.Map.Entry<String, Segments> e) { return size() > 256; }
    };

    /** Values bright, labels dimmed; drawn in sections of the same colour. */
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

    /** Digits and what belongs to numbers (sign, comma, colon in times, %). */
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

    /** A single dynamic colour (e.g. for areas) matching the mode. */
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
