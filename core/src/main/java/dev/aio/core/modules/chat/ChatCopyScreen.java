package dev.aio.core.modules.chat;

import dev.aio.core.i18n.Lang;

import dev.aio.core.AioCore;
import dev.aio.core.platform.Keys;
import dev.aio.core.platform.Renderer;
import dev.aio.core.ui.Theme;
import dev.aio.core.ui.UiScreen;

import java.util.List;

/**
 * Chatverlauf zum Markieren und Kopieren: Zeilen anklicken oder ziehen (Shift erweitert),
 * Strg+C oder "Kopieren" legt sie in die Zwischenablage.
 */
public final class ChatCopyScreen implements UiScreen {
    private static final int LINE = 11;

    private final AioCore core;
    private final ChatHistory history;
    private final boolean withTime;
    private List<ChatHistory.Entry> entries;
    private int anchor = -1, focus = -1;
    private boolean dragging;
    private float scroll = Float.MAX_VALUE;
    private int listTop, listBottom, listX, listW;
    private float[] copyBtn = new float[4], allBtn = new float[4];
    private String status;
    private long statusUntil;

    public ChatCopyScreen(AioCore core, ChatHistory history, boolean withTime) {
        this.core = core;
        this.history = history;
        this.withTime = withTime;
        this.entries = history.entries();
    }

    @Override public String title() { return Lang.tr("aio.ui.chat_copy.title", "Copy chat"); }

    private String line(int i) {
        ChatHistory.Entry e = entries.get(i);
        return withTime ? "[" + e.time() + "] " + e.text() : e.text();
    }

    @Override public void render(Renderer r, int mx, int my, float delta) {
        int w = r.screenWidth(), h = r.screenHeight();
        r.fill(0, 0, w, h, Theme.OVERLAY);
        int pw = Math.min(w - 20, 520), ph = Math.min(h - 20, 300);
        int px = (w - pw) / 2, py = (h - ph) / 2;
        r.fill(px, py, px + pw, py + ph, Theme.PANEL);
        Theme.accentBar(r, core, px, py, px + pw, py + 2);
        r.text(Lang.tr("aio.ui.chat_copy.copy_chat", "Copy chat"), px + 10, py + 9, Theme.TEXT, true);
        r.text(Lang.tr("aio.ui.chat_copy.click_drag_selects_shift_extends", "Click/drag selects, Shift extends, Ctrl+C copies"), px + 10, py + 21, Theme.TEXT_MUTED, false);
        copyBtn = new float[]{px + pw - 150, py + 8, px + pw - 82, py + 22};
        allBtn = new float[]{px + pw - 78, py + 8, px + pw - 8, py + 22};
        drawButton(r, copyBtn, Lang.tr("aio.ui.chat_copy.copy", "Copy"), mx, my, true);
        drawButton(r, allBtn, Lang.tr("aio.ui.chat_copy.all", "All"), mx, my, false);

        listX = px + 8;
        listW = pw - 16;
        listTop = py + 34;
        listBottom = py + ph - 8;
        int visible = (listBottom - listTop) / LINE;
        float maxScroll = Math.max(0, entries.size() - visible);
        scroll = Math.max(0, Math.min(maxScroll, scroll));
        int first = (int) scroll;
        int lo = Math.min(anchor, focus), hi = Math.max(anchor, focus);
        r.enableScissor(listX, listTop, listX + listW, listBottom);
        for (int i = first; i < entries.size() && i < first + visible + 1; i++) {
            float y = listTop + (i - first) * LINE;
            boolean sel = anchor >= 0 && i >= lo && i <= hi;
            if (sel) r.fill(listX, y, listX + listW, y + LINE, dev.aio.core.util.Colors.withAlpha(Theme.accent(core), 0x60));
            r.text(r.trim(line(i), listW - 6), listX + 3, y + 1, sel ? Theme.TEXT : 0xFFDDDDDD, false);
        }
        r.disableScissor();
        if (entries.isEmpty()) r.text(Lang.tr("aio.ui.chat_copy.no_messages_received_yet", "No messages received yet."), listX + 3, listTop + 2, Theme.TEXT_DIM, false);
        if (status != null && System.currentTimeMillis() < statusUntil) {
            r.centeredText(status, px + pw / 2f, py + ph - 18, Theme.accent(core), true);
        }
    }

    private void drawButton(Renderer r, float[] b, String label, int mx, int my, boolean accent) {
        boolean hover = mx >= b[0] && mx <= b[2] && my >= b[1] && my <= b[3];
        r.fill(b[0], b[1], b[2], b[3], accent ? Theme.accent(core) : hover ? 0xFF33333E : 0xFF24242C);
        r.centeredText(label, (b[0] + b[2]) / 2, b[1] + 3, Theme.TEXT, false);
    }

    private int indexAt(double my) {
        if (my < listTop || my > listBottom) return -1;
        int i = (int) scroll + (int) ((my - listTop) / LINE);
        return i >= 0 && i < entries.size() ? i : -1;
    }

    private static boolean in(float[] b, double x, double y) { return x >= b[0] && x <= b[2] && y >= b[1] && y <= b[3]; }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (in(copyBtn, mx, my)) { copySelection(); return true; }
        if (in(allBtn, mx, my)) {
            anchor = 0;
            focus = entries.size() - 1;
            copySelection();
            return true;
        }
        int i = indexAt(my);
        if (i < 0 || mx < listX || mx > listX + listW) return false;
        boolean shift = core.platform().isKeyDown(Keys.LEFT_SHIFT) || core.platform().isKeyDown(Keys.RIGHT_SHIFT);
        if (shift && anchor >= 0) focus = i;
        else anchor = focus = i;
        dragging = true;
        return true;
    }

    @Override public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (!dragging) return false;
        if (my < listTop) scroll -= 0.5f;
        if (my > listBottom) scroll += 0.5f;
        int i = indexAt(Math.max(listTop, Math.min(listBottom - 1, my)));
        if (i >= 0) focus = i;
        return true;
    }

    @Override public boolean mouseReleased(double mx, double my, int button) {
        dragging = false;
        return true;
    }

    @Override public boolean mouseScrolled(double x, double y, double amount) {
        scroll -= (float) amount * 3;
        return true;
    }

    @Override public boolean keyPressed(String key) {
        boolean ctrl = core.platform().isKeyDown(Keys.LEFT_CONTROL) || core.platform().isKeyDown(Keys.RIGHT_CONTROL);
        if (ctrl && Keys.C.equals(key)) { copySelection(); return true; }
        if (ctrl && "key.keyboard.a".equals(key)) {
            anchor = 0;
            focus = entries.size() - 1;
            return true;
        }
        return false;
    }

    /** Markierte Zeilen als Text (ohne Farbcodes). */
    String selectionText() {
        if (anchor < 0 || entries.isEmpty()) return "";
        int lo = Math.max(0, Math.min(anchor, focus)), hi = Math.min(entries.size() - 1, Math.max(anchor, focus));
        StringBuilder sb = new StringBuilder();
        for (int i = lo; i <= hi; i++) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(line(i).replaceAll("§.", ""));
        }
        return sb.toString();
    }

    private void copySelection() {
        String text = selectionText();
        if (text.isEmpty()) return;
        core.platform().setClipboard(text);
        int n = Math.abs(focus - anchor) + 1;
        status = n + (n == 1 ? Lang.tr("aio.ui.chat_copy.line", " line") : Lang.tr("aio.ui.chat_copy.lines", " lines")) + " copied";
        statusUntil = System.currentTimeMillis() + 2000;
    }

    // Für Tests
    void select(int from, int to) {
        anchor = from;
        focus = to;
    }
}
