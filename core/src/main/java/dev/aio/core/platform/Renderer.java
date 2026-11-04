package dev.aio.core.platform;

/**
 * Zeichen-API, die der Kern benutzt. Der Adapter implementiert sie pro MC-Version
 * (z. B. über {@code DrawContext}). Farben sind immer ARGB.
 */
public interface Renderer {
    int screenWidth();

    int screenHeight();

    int textWidth(String text);

    int fontHeight();

    void text(String text, float x, float y, int argb, boolean shadow);

    void fill(float x1, float y1, float x2, float y2, int argb);

    /** Zeichnet ein Item-Icon (16x16). {@code overlay} zeigt Anzahl und Haltbarkeitsbalken. */
    void item(ItemRef item, float x, float y, boolean overlay);

    /** Zeichnet ein Sprite aus dem GUI-Atlas, z. B. {@code minecraft:mob_effect/speed}. */
    void sprite(String id, float x, float y, int width, int height);

    void push();

    void pop();

    void translate(float x, float y);

    void scale(float factor);

    /** Getrennte Skalierung für Breite und Höhe. */
    default void scale(float sx, float sy) { scale(sx); }

    /** Beschränkt das Zeichnen auf ein Rechteck (GUI-Koordinaten, ohne Transformation). */
    void enableScissor(int x1, int y1, int x2, int y2);

    void disableScissor();

    // ---- Hilfsfunktionen auf Basis der Primitive ----

    default void outline(float x1, float y1, float x2, float y2, float thickness, int argb) {
        fill(x1, y1, x2, y1 + thickness, argb);
        fill(x1, y2 - thickness, x2, y2, argb);
        fill(x1, y1 + thickness, x1 + thickness, y2 - thickness, argb);
        fill(x2 - thickness, y1 + thickness, x2, y2 - thickness, argb);
    }

    /** Kürzt Text mit "…", damit er in {@code maxWidth} passt. */
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
            fill(x1, y, x2, Math.min(y2, y + 1), dev.aio.core.util.Colors.lerp(top, bottom, (y - y1) / h));
        }
    }

    default void centeredText(String text, float cx, float y, int argb, boolean shadow) {
        text(text, cx - textWidth(text) / 2f, y, argb, shadow);
    }
}
