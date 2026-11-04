package dev.aio.core.ui;

import dev.aio.core.platform.Renderer;

/**
 * Klassischer Minecraft-Look für Item-Liste und Rezeptansicht: graue Fenster mit heller und
 * dunkler Kante, eingelassene Felder, Slots, Vanilla-Knöpfe, Pfeile. Alles selbst gezeichnet,
 * nur die Knopf-Grafik kommt aus dem Spiel (damit Ressourcenpakete greifen).
 */
public final class McGui {
    public static final int BG = 0xFFC6C6C6;
    public static final int LIGHT = 0xFFFFFFFF;
    public static final int SHADOW = 0xFF555555;
    public static final int BORDER = 0xFF000000;
    public static final int SLOT = 0xFF8B8B8B;
    public static final int SLOT_DARK = 0xFF373737;
    public static final int TEXT_DARK = 0xFF404040;

    private McGui() {}

    /** Fenster mit schwarzem Rand, abgerundeten Ecken und Licht/Schatten-Kante. */
    public static void panel(Renderer r, float x1, float y1, float x2, float y2) {
        r.fill(x1 + 2, y1, x2 - 2, y1 + 1, BORDER);
        r.fill(x1 + 2, y2 - 1, x2 - 2, y2, BORDER);
        r.fill(x1, y1 + 2, x1 + 1, y2 - 2, BORDER);
        r.fill(x2 - 1, y1 + 2, x2, y2 - 2, BORDER);
        r.fill(x1 + 1, y1 + 1, x1 + 2, y1 + 2, BORDER);
        r.fill(x2 - 2, y1 + 1, x2 - 1, y1 + 2, BORDER);
        r.fill(x1 + 1, y2 - 2, x1 + 2, y2 - 1, BORDER);
        r.fill(x2 - 2, y2 - 2, x2 - 1, y2 - 1, BORDER);
        r.fill(x1 + 1, y1 + 2, x2 - 1, y2 - 2, BG);
        r.fill(x1 + 2, y1 + 1, x2 - 2, y2 - 1, BG);
        r.fill(x1 + 2, y1 + 1, x2 - 3, y1 + 3, LIGHT);
        r.fill(x1 + 1, y1 + 2, x1 + 3, y2 - 3, LIGHT);
        r.fill(x1 + 3, y2 - 3, x2 - 2, y2 - 1, SHADOW);
        r.fill(x2 - 3, y1 + 3, x2 - 1, y2 - 2, SHADOW);
    }

    /** Eingelassene Fläche (z. B. Rezept-Kasten, Beschriftungsleiste). */
    public static void inset(Renderer r, float x1, float y1, float x2, float y2) {
        r.fill(x1, y1, x2, y2, SLOT);
        r.fill(x1, y1, x2 - 1, y1 + 1, SLOT_DARK);
        r.fill(x1, y1, x1 + 1, y2 - 1, SLOT_DARK);
        r.fill(x1 + 1, y2 - 1, x2, y2, LIGHT);
        r.fill(x2 - 1, y1 + 1, x2, y2, LIGHT);
    }

    /** Flacher Rahmen mit heller Füllung (Rezept-Kasten wie in Rezeptbüchern). */
    public static void box(Renderer r, float x1, float y1, float x2, float y2) {
        r.fill(x1, y1, x2, y2, 0xFFB7B7B7);
        r.outline(x1, y1, x2, y2, 1, 0xFF8B8B8B);
        r.fill(x1 + 1, y1 + 1, x2 - 1, y1 + 2, 0xFFD2D2D2);
    }

    /** Item-Slot 18×18. */
    public static void slot(Renderer r, float x, float y) { slot(r, x, y, 18); }

    public static void slot(Renderer r, float x, float y, int size) {
        r.fill(x, y, x + size, y + size, SLOT);
        r.fill(x, y, x + size - 1, y + 1, SLOT_DARK);
        r.fill(x, y, x + 1, y + size - 1, SLOT_DARK);
        r.fill(x + 1, y + size - 1, x + size, y + size, LIGHT);
        r.fill(x + size - 1, y + 1, x + size, y + size, LIGHT);
    }

    /** Vanilla-Knopf (Grafik aus dem Spiel, mit Rand dazu gezeichnet), gibt true bei Maus darüber. */
    public static boolean button(Renderer r, float x1, float y1, float x2, float y2, int mx, int my, boolean enabled) {
        boolean hover = enabled && mx >= x1 && mx < x2 && my >= y1 && my < y2;
        String sprite = !enabled ? "minecraft:widget/button_disabled" : hover ? "minecraft:widget/button_highlighted" : "minecraft:widget/button";
        r.sprite(sprite, x1, y1, (int) (x2 - x1), (int) (y2 - y1));
        return hover;
    }

    /** Dreieck-Pfeil nach links oder rechts, zentriert in (cx, cy). */
    public static void arrow(Renderer r, float cx, float cy, boolean right, int color) {
        for (int i = 0; i < 4; i++) {
            float h = 3.5f - i;
            float x = right ? cx - 1.5f + i : cx + 1.5f - i;
            r.fill(x, cy - h, x + 1, cy + h, color);
        }
    }

    /** Knopf mit Pfeil. */
    public static boolean arrowButton(Renderer r, float x1, float y1, float x2, float y2, boolean right, int mx, int my, boolean enabled) {
        boolean hover = button(r, x1, y1, x2, y2, mx, my, enabled);
        arrow(r, (x1 + x2) / 2f, (y1 + y2) / 2f, right, enabled ? 0xFFFFFFFF : 0xFFA0A0A0);
        return hover;
    }

    /** Großer grauer Rezept-Pfeil (→) zwischen Zutaten und Ergebnis, 22×15. */
    public static void recipeArrow(Renderer r, float x, float y) {
        int c = 0xFF8B8B8B;
        r.fill(x, y + 5, x + 14, y + 10, c);
        for (int i = 0; i < 8; i++) r.fill(x + 14 + i, y + i, x + 15 + i, y + 15 - i, c);
    }

    /** Schraubenschlüssel-Symbol (Einstellungen), 12×12. */
    public static void wrench(Renderer r, float x, float y, int color) {
        for (int i = 0; i < 7; i++) r.fill(x + 1 + i, y + 10 - i, x + 3 + i, y + 12 - i, color);
        r.fill(x + 7, y + 1, x + 12, y + 6, color);
        r.fill(x + 9, y + 2, x + 11, y + 4, 0xFF5A5A5A);
    }

    /** Vanilla-Textfeld: schwarz mit grauem (fokussiert weißem) Rand. */
    public static void textField(Renderer r, float x1, float y1, float x2, float y2, boolean focused) {
        r.fill(x1, y1, x2, y2, focused ? 0xFFFFFFFF : 0xFFA0A0A0);
        r.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, 0xFF000000);
    }
}
