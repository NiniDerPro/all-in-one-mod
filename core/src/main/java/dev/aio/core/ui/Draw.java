package dev.aio.core.ui;

import dev.aio.core.platform.Renderer;
import dev.aio.core.util.Colors;

/** Shapes made of rectangles: rounded areas, frames, pixel icons. */
public final class Draw {
    private Draw() {}

    /** Area with rounded corners (radius 1–3 pixels). */
    public static void round(Renderer r, float x1, float y1, float x2, float y2, int radius, int color) {
        if ((color >>> 24) == 0) return;
        if (radius <= 0 || x2 - x1 < radius * 2 || y2 - y1 < radius * 2) {
            r.fill(x1, y1, x2, y2, color);
            return;
        }
        r.fill(x1 + radius, y1, x2 - radius, y2, color);
        for (int i = 0; i < radius; i++) {
            // per column slightly less tall at the edge
            float inset = radius - (float) Math.sqrt(radius * radius - (radius - i - 0.5) * (radius - i - 0.5));
            inset = Math.round(inset);
            r.fill(x1 + i, y1 + inset, x1 + i + 1, y2 - inset, color);
            r.fill(x2 - i - 1, y1 + inset, x2 - i, y2 - inset, color);
        }
    }

    /** 1-pixel frame with rounded corners. */
    public static void roundOutline(Renderer r, float x1, float y1, float x2, float y2, int color) {
        r.fill(x1 + 1, y1, x2 - 1, y1 + 1, color);
        r.fill(x1 + 1, y2 - 1, x2 - 1, y2, color);
        r.fill(x1, y1 + 1, x1 + 1, y2 - 1, color);
        r.fill(x2 - 1, y1 + 1, x2, y2 - 1, color);
    }

    /** Switch (18×10): knob glides with {@code t} 0..1 from left to right. */
    public static void toggle(Renderer r, float x, float y, float t, int onColor, boolean dim) {
        int track = Colors.lerp(Theme.TRACK, onColor, t);
        if (dim) track = Colors.withAlpha(track, 0x90);
        round(r, x, y, x + 18, y + 10, 3, track);
        float kx = x + 1 + 8 * t;
        round(r, kx, y + 1, kx + 8, y + 9, 2, 0xFFFFFFFF);
    }

    /** Key as a cap, e.g. "F4". Returns the width. */
    public static float keycap(Renderer r, String label, float x, float y, int textColor) {
        float w = r.textWidth(label) + 8;
        round(r, x, y, x + w, y + 11, 2, 0xFF242936);
        r.fill(x + 1, y + 10, x + w - 1, y + 11, 0xFF10131A);
        r.text(label, x + 4, y + 2, textColor, false);
        return w;
    }

    // ---- Pixel-Icons (8×8), '#' = gesetzt ----

    public static final String[] GEAR = {
            "..#..#..",
            ".######.",
            ".##..##.",
            "##....##",
            "##....##",
            ".##..##.",
            ".######.",
            "..#..#.."};
    public static final String[] SEARCH = {
            ".####...",
            "#....#..",
            "#....#..",
            "#....#..",
            ".####...",
            ".....#..",
            "......#.",
            ".......#"};
    public static final String[] CLOSE = {
            "#......#",
            ".#....#.",
            "..#..#..",
            "...##...",
            "...##...",
            "..#..#..",
            ".#....#.",
            "#......#"};
    public static final String[] LAYOUT = {
            "########",
            "#..#...#",
            "#..#...#",
            "####...#",
            "#..#...#",
            "#..#####",
            "#..#...#",
            "########"};
    public static final String[] CHEVRON_LEFT = {
            "....#...",
            "...#....",
            "..#.....",
            ".#......",
            ".#......",
            "..#.....",
            "...#....",
            "....#..."};
    public static final String[] CHEVRON_RIGHT = {
            "...#....",
            "....#...",
            ".....#..",
            "......#.",
            "......#.",
            ".....#..",
            "....#...",
            "...#...."};
    public static final String[] CHEVRON_DOWN = {
            "........",
            "........",
            "#......#",
            ".#....#.",
            "..#..#..",
            "...##...",
            "........",
            "........"};
    public static final String[] SORT = {
            "..#.....",
            ".###....",
            "#.#.#...",
            "..#..#..",
            "..#..#..",
            "...#.#.#",
            "....###.",
            ".....#.."};
    public static final String[] PENCIL = {
            "......##",
            ".....###",
            "....###.",
            "...###..",
            "..###...",
            ".###....",
            "##......",
            "#......."};

    public static final String[] TRASH = {
            "..####..",
            "########",
            "........",
            ".#.##.#.",
            ".#.##.#.",
            ".#.##.#.",
            ".#.##.#.",
            ".######."};

    public static final String[] PLUS = {
            "...##...",
            "...##...",
            "...##...",
            "########",
            "########",
            "...##...",
            "...##...",
            "...##..."};

    public static final String[] RESET = {
            "..####..",
            ".#....#.",
            "#......#",
            "#.......",
            "#....###",
            "#.....##",
            ".#....#.",
            "..####.."};

    public static void icon(Renderer r, String[] icon, float x, float y, int color) {
        for (int row = 0; row < icon.length; row++) {
            String line = icon[row];
            int start = -1;
            for (int col = 0; col <= line.length(); col++) {
                boolean on = col < line.length() && line.charAt(col) == '#';
                if (on && start < 0) start = col;
                if (!on && start >= 0) {
                    r.fill(x + start, y + row, x + col, y + row + 1, color);
                    start = -1;
                }
            }
        }
    }

    /** Bold text (like Minecraft: twice offset by one pixel). */
    public static void bold(Renderer r, String text, float x, float y, int color) {
        r.text(text, x, y, color, false);
        r.text(text, x + 1, y, color, false);
    }

    public static int boldWidth(Renderer r, String text) { return r.textWidth(text) + 1; }
}
