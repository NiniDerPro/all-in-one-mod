package dev.aio.core.platform;

import dev.aio.core.util.Colors;

/**
 * Drawing API that the core uses. The adapter implements it per MC version
 * (e.g. via {@code DrawContext}). Colours are always ARGB.
 */
public interface Renderer {
    int screenWidth();

    int screenHeight();

    int textWidth(String text);

    int fontHeight();

    void text(String text, float x, float y, int argb, boolean shadow);

    /**
     * Text with its own colour per character (rainbow, gradient). The adapter draws it in a single
     * call; the default implementation draws character by character.
     *
     * @param colors  ARGB per code point, alpha of the first entry applies to all
     * @param offsets x offset per code point (for the character-by-character implementation)
     */
    default void coloredText(String text, int[] colors, float[] offsets, float x, float y, boolean shadow) {
        for (int i = 0, idx = 0; i < text.length() && idx < colors.length; idx++) {
            int cp = text.codePointAt(i);
            text(new String(Character.toChars(cp)), x + offsets[idx], y, colors[idx], shadow);
            i += Character.charCount(cp);
        }
    }

    void fill(float x1, float y1, float x2, float y2, int argb);

    /** Draws an item icon (16x16). {@code overlay} shows count and durability bar. */
    void item(ItemRef item, float x, float y, boolean overlay);

    /** Draws a sprite from the GUI atlas, e.g. {@code minecraft:mob_effect/speed}. */
    void sprite(String id, float x, float y, int width, int height);

    /**
     * Draws a pixel image (ARGB, w×h) scaled to dw×dh. The image is only uploaded again
     * when {@code version} changed; that way it costs only one rectangle per frame.
     */
    default void image(String key, int[] argb, int w, int h, int version, float x, float y, float dw, float dh) {}

    /** Sprite mit Farbe/Transparenz (ARGB-Multiplikator). */
    default void sprite(String id, float x, float y, int width, int height, int argb) { sprite(id, x, y, width, height); }

    void push();

    void pop();

    void translate(float x, float y);

    void scale(float factor);

    /** Separate scaling for width and height. */
    default void scale(float sx, float sy) { scale(sx); }

    /** Restricts drawing to a rectangle (GUI coordinates, without transformation). */
    void enableScissor(int x1, int y1, int x2, int y2);

    void disableScissor();

    // ---- Helper functions based on the primitives ----

    default void outline(float x1, float y1, float x2, float y2, float thickness, int argb) {
        fill(x1, y1, x2, y1 + thickness, argb);
        fill(x1, y2 - thickness, x2, y2, argb);
        fill(x1, y1 + thickness, x1 + thickness, y2 - thickness, argb);
        fill(x2 - thickness, y1 + thickness, x2, y2 - thickness, argb);
    }

    /** Shortens text with "…" so that it fits in {@code maxWidth}. */
    default String trim(String text, float maxWidth) {
        if (textWidth(text) <= maxWidth) return text;
        String ellipsis = "…";
        float target = maxWidth - textWidth(ellipsis);
        int end = text.length();
        while (end > 0 && textWidth(text.substring(0, end)) > target) end--;
        return text.substring(0, end) + ellipsis;
    }

    /** Vertikaler Farbverlauf (zeilenweise aus fill zusammengesetzt). */
    default void fillGradient(float x1, float y1, float x2, float y2, int top, int bottom) {
        float h = y2 - y1;
        if (h <= 0) return;
        if (top == bottom) {
            fill(x1, y1, x2, y2, top);
            return;
        }
        for (float y = y1; y < y2; y += 1) {
            fill(x1, y, x2, Math.min(y2, y + 1), Colors.lerp(top, bottom, (y - y1) / h));
        }
    }

    default void centeredText(String text, float cx, float y, int argb, boolean shadow) {
        text(text, cx - textWidth(text) / 2f, y, argb, shadow);
    }
}
