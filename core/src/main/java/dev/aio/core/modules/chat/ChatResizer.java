package dev.aio.core.modules.chat;

import dev.aio.core.AioCore;
import dev.aio.core.platform.Renderer;
import dev.aio.core.ui.Theme;

/**
 * Größe des Chats direkt im offenen Chat (T) ändern: an der oberen oder rechten Kante bzw. der
 * Ecke oben rechts ziehen. Die Position (unten links) legt Minecraft fest.
 */
public final class ChatResizer {
    private static final float GRIP = 3f;
    private static final int RIGHT = 1, TOP = 2;

    private final AioCore core;
    private int edges;
    private float startMx, startMy, startW, startH;

    public ChatResizer(AioCore core) { this.core = core; }

    private FlexChatModule chat() {
        FlexChatModule c = core.module(FlexChatModule.class);
        return c != null && c.isActive() ? c : null;
    }

    /** x, y, Breite, Höhe der Chatbox. */
    private float[] box(Renderer r, FlexChatModule c) {
        float bottom = r.screenHeight() - 40;
        return new float[]{2, bottom - c.boxHeight(), c.boxWidth(), c.boxHeight()};
    }

    private int edgesAt(Renderer r, double mx, double my) {
        FlexChatModule c = chat();
        if (c == null) return 0;
        float[] b = box(r, c);
        boolean inX = mx >= b[0] && mx <= b[0] + b[2] + GRIP, inY = my >= b[1] - GRIP && my <= b[1] + b[3];
        int e = 0;
        if (inY && Math.abs(mx - (b[0] + b[2])) <= GRIP) e |= RIGHT;
        if (inX && Math.abs(my - b[1]) <= GRIP) e |= TOP;
        return e;
    }

    public boolean isResizing() { return edges != 0; }

    /** Rahmen und Griffe, sobald die Maus an einer Kante ist oder gezogen wird. */
    public void render(Renderer r, int mx, int my) {
        FlexChatModule c = chat();
        if (c == null) return;
        int hot = edges != 0 ? edges : edgesAt(r, mx, my);
        if (hot == 0) return;
        float[] b = box(r, c);
        int accent = Theme.accent(core);
        r.outline(b[0] - 1, b[1] - 1, b[0] + b[2] + 1, b[1] + b[3] + 1, 1, accent);
        float x2 = b[0] + b[2] + 1, y1 = b[1] - 1;
        handle(r, x2, y1, hot == (RIGHT | TOP));
        handle(r, (b[0] + x2) / 2f, y1, hot == TOP);
        handle(r, x2, b[1] + b[3] / 2f, hot == RIGHT);
        String size = Math.round(b[2]) + " × " + Math.round(b[3]);
        r.text(size, b[0] + 2, b[1] - 12, 0xFFFFFFFF, true);
    }

    private void handle(Renderer r, float x, float y, boolean hot) {
        r.fill(x - 3, y - 3, x + 3, y + 3, 0xFF000000);
        r.fill(x - 2.5f, y - 2.5f, x + 2.5f, y + 2.5f, hot ? Theme.accent(core) : 0xFFFFFFFF);
    }

    /** Linksklick an einer Kante startet das Ziehen. */
    public boolean mouseClicked(Renderer r, double mx, double my, int button) {
        if (button != 0) return false;
        int e = edgesAt(r, mx, my);
        if (e == 0) return false;
        float[] b = box(r, chat());
        edges = e;
        startMx = (float) mx;
        startMy = (float) my;
        startW = b[2];
        startH = b[3];
        return true;
    }

    public boolean mouseDragged(double mx, double my) {
        FlexChatModule c = chat();
        if (edges == 0 || c == null) return false;
        float w = (edges & RIGHT) != 0 ? startW + (float) mx - startMx : startW;
        float h = (edges & TOP) != 0 ? startH - ((float) my - startMy) : startH;
        c.resizeTo(Math.max(40, w), Math.max(9, h));
        return true;
    }

    public boolean mouseReleased() {
        if (edges == 0) return false;
        edges = 0;
        core.config().save();
        return true;
    }
}
