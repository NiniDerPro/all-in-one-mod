package dev.aio.core.ui;

import dev.aio.core.platform.Renderer;

/** Shared frame for windows (like the main menu): shadow, rounded area, title. */
public final class Window {
    private Window() {}

    public static void frame(Renderer r, float x1, float y1, float x2, float y2) {
        Draw.round(r, x1 - 2, y1 - 1, x2 + 2, y2 + 3, 3, 0x40000000);
        Draw.round(r, x1, y1, x2, y2, 3, Theme.PANEL);
        Draw.roundOutline(r, x1, y1, x2, y2, Theme.LINE);
    }

    public static void title(Renderer r, String title, float x, float y) {
        Draw.bold(r, title, x, y, Theme.TEXT);
    }
}
