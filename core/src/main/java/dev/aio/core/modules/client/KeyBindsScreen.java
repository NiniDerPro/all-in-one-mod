package dev.aio.core.modules.client;

import dev.aio.core.AioCore;
import dev.aio.core.i18n.Lang;
import dev.aio.core.platform.KeyBinding;
import dev.aio.core.platform.Keys;
import dev.aio.core.platform.Renderer;
import dev.aio.core.ui.TextEdit;
import dev.aio.core.ui.Theme;
import dev.aio.core.util.Colors;
import dev.aio.core.ui.Draw;
import dev.aio.core.ui.Window;
import dev.aio.core.ui.UiScreen;

import java.util.ArrayList;
import java.util.List;

/**
 * Key bindings with search: typing filters by name, category or key. Filters for conflicts,
 * unbound and free keys, sorting and "Reset all". Click on a key and then press
 * the new key (or mouse button); Escape removes the binding.
 */
public final class KeyBindsScreen implements UiScreen {
    private static final int ROW = 14;

    private final AioCore core;
    private final KeyBindIndex index;
    private String query = "";
    private boolean queryAll;
    private KeyBindIndex.Filter filter = KeyBindIndex.Filter.ALL;
    private KeyBindIndex.Sort sort = KeyBindIndex.Sort.DEFAULT;
    private KeyBinding listening;
    private boolean confirmReset;
    /** State of confirmReset before the current click. */
    private boolean armed;
    private float scroll;

    /** Click areas of the last frame. */
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

    @Override public String title() { return Lang.tr("aio.ui.key_binds.title", "Key Binds"); }

    @Override public boolean returnsToPrevious() { return true; }

    @Override public boolean replacesParent() { return true; }

    @Override public void onOpen() { index.refresh(); }

    @Override public void render(Renderer r, int mx, int my, float delta) {
        hits.clear();
        int w = r.screenWidth(), h = r.screenHeight();
        r.fill(0, 0, w, h, Theme.OVERLAY);
        float pw = Math.min(w - 16, 380), px = (w - pw) / 2f, py = 8, ph = h - 16;
        Window.frame(r, px, py, px + pw, py + ph);
        Window.title(r, Lang.tr("aio.ui.key_binds.key_binds", "Key Binds"), px + 8, py + 8);

        // Suchfeld
        float sx = px + 8, sy = py + 22, sw = pw - 16;
        Draw.round(r, sx, sy, sx + sw, sy + 14, 2, 0xFF1E2230);
        Draw.roundOutline(r, sx, sy, sx + sw, sy + 14, Colors.withAlpha(Theme.accent(core), 0xC0));
        boolean caret = !queryAll && (System.currentTimeMillis() / 500) % 2 == 0;
        if (queryAll) TextEdit.drawSelection(r, sx + 4, sy + 3, r.trim(query, sw - 8));
        String shown = query.isEmpty() ? Lang.tr("aio.ui.key_binds.search_name_category_or_key", "Search: name, category or key…") : query + (caret ? "_" : "");
        r.text(r.trim(shown, sw - 8), sx + 4, sy + 3, query.isEmpty() ? Theme.TEXT_MUTED : Theme.TEXT, false);

        // Filters, sorting, reset
        float bx = px + 8, by = sy + 18;
        for (KeyBindIndex.Filter f : KeyBindIndex.Filter.values()) {
            bx += button(r, bx, by, f.label, f == filter, mx, my, () -> { filter = f; scroll = 0; }) + 3;
        }
        String sortLabel = Lang.tr("aio.ui.key_binds.sort", "Sort: %s", sort.label);
        float sortW = r.textWidth(sortLabel) + 10;
        String resetLabel = confirmReset ? Lang.tr("aio.ui.key_binds.really", "Really?") : Lang.tr("aio.ui.key_binds.reset_all", "Reset all");
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

        String hint = listening != null ? Lang.tr("aio.ui.key_binds.press_a_new_key_escape", "Press a new key · Escape: unbind") : Lang.tr("aio.ui.key_binds.click_a_key_to_change", "Click a key to change it · Scroll: browse");
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
                Draw.round(r, kx, y + 1, kx + keyW, y + ROW - 1, 2, isListening ? Theme.accent(core) : keyHover ? Theme.CARD_HOVER : Theme.CARD);
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
        if (entries.isEmpty()) r.text(Lang.tr("aio.ui.key_binds.nothing_found", "Nothing found."), x, listTop + 4, Theme.TEXT_DIM, false);
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

    /** Draws a button and remembers its area. @return width */
    private float button(Renderer r, float x, float y, String label, boolean active, int mx, int my, Runnable action) {
        float bw = r.textWidth(label) + 10;
        boolean hover = mx >= x && mx <= x + bw && my >= y && my <= y + 13;
        Draw.round(r, x, y, x + bw, y + 13, 2, active ? Theme.accent(core) : hover ? Theme.CARD_HOVER : Theme.CARD);
        r.text(label, x + 5, y + 3, Theme.TEXT, false);
        hits.add(new Hit(x, y, x + bw, y + 13, action));
        return bw;
    }

    // ---- Eingabe ----

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (listening != null) {
            // Bind mouse buttons (left click only outside the list, otherwise cancelling would be impossible)
            if (button != 0) {
                assign(Keys.mouse(button));
                return true;
            }
        }
        for (Hit h : hits) {
            if (h.in(mx, my) && button == 0) {
                armed = confirmReset;
                confirmReset = false; // only a second click on "Reset" confirms
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

    /** The character for the key just bound still arrives as charTyped and must not go into the search. */
    private boolean swallowChar;

    @Override public boolean keyPressed(String key) {
        swallowChar = false;
        if (listening != null) {
            assign(Keys.ESCAPE.equals(key) ? Keys.NONE : key);
            swallowChar = true;
            return true;
        }
        var res = TextEdit.key(core.platform(), query, queryAll, key);
        if (!res.text().equals(query)) scroll = 0;
        query = res.text().length() > 40 ? res.text().substring(0, 40) : res.text();
        queryAll = res.allSelected();
        return res.handled();
    }

    @Override public boolean charTyped(String text) {
        if (swallowChar) {
            swallowChar = false;
            return true;
        }
        if (listening != null || text.isEmpty() || text.codePointAt(0) < 32) return false;
        query = TextEdit.type(query, queryAll, text);
        if (query.length() > 40) query = query.substring(0, 40);
        queryAll = false;
        scroll = 0;
        return true;
    }
}
