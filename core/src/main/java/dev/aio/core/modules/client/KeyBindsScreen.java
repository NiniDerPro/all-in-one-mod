package dev.aio.core.modules.client;

import dev.aio.core.AioCore;
import dev.aio.core.platform.KeyBinding;
import dev.aio.core.platform.Keys;
import dev.aio.core.platform.Renderer;
import dev.aio.core.ui.Theme;
import dev.aio.core.ui.UiScreen;

import java.util.ArrayList;
import java.util.List;

/**
 * Tastenbelegung mit Suche: Tippen filtert nach Name, Kategorie oder Taste. Filter für Konflikte,
 * unbelegte und freie Tasten, Sortierung und "Alle zurücksetzen". Klick auf eine Taste und dann
 * die neue Taste (oder Maustaste) drücken; Escape entfernt die Belegung.
 */
public final class KeyBindsScreen implements UiScreen {
    private static final int ROW = 14;

    private final AioCore core;
    private final KeyBindIndex index;
    private String query = "";
    private KeyBindIndex.Filter filter = KeyBindIndex.Filter.ALL;
    private KeyBindIndex.Sort sort = KeyBindIndex.Sort.DEFAULT;
    private KeyBinding listening;
    private boolean confirmReset;
    /** Stand von confirmReset vor dem aktuellen Klick. */
    private boolean armed;
    private float scroll;

    /** Klickflächen des letzten Frames. */
    private record Hit(float x1, float y1, float x2, float y2, Runnable action) {
        boolean in(double x, double y) { return x >= x1 && x <= x2 && y >= y1 && y <= y2; }
    }
    private final List<Hit> hits = new ArrayList<>();
    private float listTop, listBottom;
    private int contentHeight;

    public KeyBindsScreen(AioCore core) {
        this.core = core;
        this.index = new KeyBindIndex(core);
        index.refresh();
    }

    @Override public String title() { return "Tastenbelegung"; }

    @Override public boolean returnsToPrevious() { return true; }

    @Override public boolean replacesParent() { return true; }

    @Override public void onOpen() { index.refresh(); }

    @Override public void render(Renderer r, int mx, int my, float delta) {
        hits.clear();
        int w = r.screenWidth(), h = r.screenHeight();
        r.fill(0, 0, w, h, Theme.OVERLAY);
        float pw = Math.min(w - 16, 380), px = (w - pw) / 2f, py = 8, ph = h - 16;
        r.fill(px, py, px + pw, py + ph, Theme.PANEL);
        Theme.accentBar(r, core, px, py, px + pw, py + 2);
        r.text("Tastenbelegung", px + 8, py + 8, Theme.TEXT, true);

        // Suchfeld
        float sx = px + 8, sy = py + 22, sw = pw - 16;
        r.fill(sx, sy, sx + sw, sy + 14, 0xF0202028);
        r.outline(sx, sy, sx + sw, sy + 14, 1, Theme.accent(core));
        boolean caret = (System.currentTimeMillis() / 500) % 2 == 0;
        String shown = query.isEmpty() ? "Suchen: Name, Kategorie oder Taste…" : query + (caret ? "_" : "");
        r.text(r.trim(shown, sw - 8), sx + 4, sy + 3, query.isEmpty() ? Theme.TEXT_MUTED : Theme.TEXT, false);

        // Filter, Sortierung, Zurücksetzen
        float bx = px + 8, by = sy + 18;
        for (KeyBindIndex.Filter f : KeyBindIndex.Filter.values()) {
            bx += button(r, bx, by, f.label, f == filter, mx, my, () -> { filter = f; scroll = 0; }) + 3;
        }
        String sortLabel = "Sortierung: " + sort.label;
        float sortW = r.textWidth(sortLabel) + 10;
        String resetLabel = confirmReset ? "Wirklich?" : "Alle zurücksetzen";
        float resetW = r.textWidth(resetLabel) + 10;
        float rx = px + pw - 8 - resetW;
        button(r, rx, by, resetLabel, confirmReset, mx, my, () -> {
            if (armed) index.resetAll();
            else confirmReset = true;
        });
        if (filter != KeyBindIndex.Filter.FREE) {
            button(r, rx - 4 - sortW, by, sortLabel, false, mx, my, () -> sort = KeyBindIndex.Sort.values()[(sort.ordinal() + 1) % KeyBindIndex.Sort.values().length]);
        }

        listTop = by + 18;
        listBottom = py + ph - 16;
        r.enableScissor((int) px, (int) listTop, (int) (px + pw), (int) listBottom);
        if (filter == KeyBindIndex.Filter.FREE) renderFree(r, px + 8, pw - 16);
        else renderList(r, px + 8, pw - 16, mx, my);
        r.disableScissor();

        String hint = listening != null ? "Neue Taste drücken · Escape: entfernen" : "Klick auf eine Taste zum Ändern · Mausrad: blättern";
        r.text(hint, px + 8, py + ph - 12, listening != null ? Theme.WARNING : Theme.TEXT_MUTED, false);
    }

    private void renderList(Renderer r, float x, float width, int mx, int my) {
        List<KeyBindIndex.Entry> entries = index.entries(query, filter, sort);
        float y = listTop - scroll;
        String lastCategory = null;
        float keyW = 96;
        for (KeyBindIndex.Entry e : entries) {
            KeyBinding b = e.binding();
            if (sort == KeyBindIndex.Sort.DEFAULT && !b.category().equals(lastCategory)) {
                lastCategory = b.category();
                y += 4;
                if (visible(y)) r.text(lastCategory, x, y + 2, Theme.accent(core), true);
                y += ROW;
            }
            if (visible(y)) {
                boolean hover = mx >= x && mx <= x + width && my >= y && my < y + ROW && my >= listTop && my <= listBottom;
                if (hover) r.fill(x - 2, y, x + width + 2, y + ROW, 0x20FFFFFF);
                String label = b.name() + (sort != KeyBindIndex.Sort.DEFAULT ? "  §8" + b.category() : "");
                r.text(r.trim(label, width - keyW - 24), x, y + 3, e.conflict() ? Theme.DANGER : Theme.TEXT, false);

                float kx = x + width - keyW - 16;
                boolean isListening = listening != null && listening.id().equals(b.id());
                String keyText = isListening ? "> ? <" : b.unbound() ? "—" : e.keyDisplay();
                boolean keyHover = mx >= kx && mx <= kx + keyW && my >= y && my < y + ROW - 1;
                r.fill(kx, y + 1, kx + keyW, y + ROW - 1, isListening ? Theme.accent(core) : keyHover ? Theme.CARD_HOVER : Theme.CARD);
                if (e.conflict()) r.outline(kx, y + 1, kx + keyW, y + ROW - 1, 1, Theme.DANGER);
                r.centeredText(r.trim(keyText, keyW - 6), kx + keyW / 2f, y + 3,
                        e.conflict() ? Theme.DANGER : b.unbound() ? Theme.TEXT_MUTED : Theme.TEXT, false);
                if (my >= listTop && my <= listBottom) {
                    hits.add(new Hit(kx, y + 1, kx + keyW, y + ROW - 1, () -> listening = b));
                }
                if (!b.isDefault()) {
                    float rx = x + width - 12;
                    boolean rh = mx >= rx && mx <= rx + 12 && my >= y && my < y + ROW;
                    r.text("↺", rx + 2, y + 3, rh ? Theme.accent(core) : Theme.TEXT_DIM, false);
                    if (my >= listTop && my <= listBottom) hits.add(new Hit(rx, y, rx + 12, y + ROW, () -> index.set(b, b.defaultKey())));
                }
            }
            y += ROW;
        }
        if (entries.isEmpty()) r.text("Nichts gefunden.", x, listTop + 4, Theme.TEXT_DIM, false);
        contentHeight = (int) (y + scroll - listTop);
    }

    private void renderFree(Renderer r, float x, float width) {
        List<String> free = index.freeKeys();
        float cx = x, y = listTop + 2 - scroll;
        for (String k : free) {
            String name = core.platform().keyName(k);
            if (!query.isEmpty() && !name.toLowerCase().contains(query.toLowerCase())) continue;
            float cw = r.textWidth(name) + 10;
            if (cx + cw > x + width) { cx = x; y += ROW; }
            if (visible(y)) {
                r.fill(cx, y, cx + cw, y + ROW - 2, Theme.CARD);
                r.text(name, cx + 5, y + 2, Theme.TEXT, false);
            }
            cx += cw + 3;
        }
        contentHeight = (int) (y + ROW + scroll - listTop);
    }

    private boolean visible(float y) { return y + ROW > listTop && y < listBottom; }

    /** Zeichnet einen Knopf und merkt sich die Fläche. @return Breite */
    private float button(Renderer r, float x, float y, String label, boolean active, int mx, int my, Runnable action) {
        float bw = r.textWidth(label) + 10;
        boolean hover = mx >= x && mx <= x + bw && my >= y && my <= y + 13;
        r.fill(x, y, x + bw, y + 13, active ? Theme.accent(core) : hover ? Theme.CARD_HOVER : Theme.CARD);
        r.text(label, x + 5, y + 3, Theme.TEXT, false);
        hits.add(new Hit(x, y, x + bw, y + 13, action));
        return bw;
    }

    // ---- Eingabe ----

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (listening != null) {
            // Maustasten belegen (Linksklick nur außerhalb der Liste, sonst wäre Abbrechen unmöglich)
            if (button != 0) {
                assign(Keys.mouse(button));
                return true;
            }
        }
        for (Hit h : hits) {
            if (h.in(mx, my) && button == 0) {
                armed = confirmReset;
                confirmReset = false; // nur ein zweiter Klick auf "Zurücksetzen" bestätigt
                h.action.run();
                return true;
            }
        }
        listening = null;
        confirmReset = false;
        return false;
    }

    private void assign(String key) {
        if (listening == null) return;
        index.set(listening, key);
        listening = null;
    }

    @Override public boolean mouseScrolled(double x, double y, double amount) {
        float max = Math.max(0, contentHeight - (listBottom - listTop));
        scroll = Math.max(0, Math.min(max, scroll - (float) amount * ROW * 2));
        return true;
    }

    @Override public boolean keyPressed(String key) {
        if (listening != null) {
            assign(Keys.ESCAPE.equals(key) ? Keys.NONE : key);
            return true;
        }
        if (Keys.BACKSPACE.equals(key)) {
            if (!query.isEmpty()) query = query.substring(0, query.length() - 1);
            scroll = 0;
            return true;
        }
        return false;
    }

    @Override public boolean charTyped(String text) {
        if (listening != null || text.isEmpty() || text.codePointAt(0) < 32) return false;
        if (query.length() < 40) query += text;
        scroll = 0;
        return true;
    }
}
