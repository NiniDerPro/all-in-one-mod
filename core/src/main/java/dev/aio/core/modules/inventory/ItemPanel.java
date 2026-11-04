package dev.aio.core.modules.inventory;

import dev.aio.core.AioCore;
import dev.aio.core.platform.ItemRef;
import dev.aio.core.platform.Keys;
import dev.aio.core.platform.Renderer;
import dev.aio.core.ui.Theme;

import java.util.List;

/**
 * Item-Liste rechts neben Inventaren: Raster aller Items, Suchfeld unten, Blättern per Mausrad.
 * Linksklick (oder R) zeigt Rezepte, Rechtsklick (oder U) zeigt Verwendungen.
 */
public final class ItemPanel {
    private static final int CELL = 18;
    private static final int SEARCH_H = 16;

    private final AioCore core;
    private final ItemListModule module;
    private List<ItemRef> all = List.of();
    private List<ItemRef> filtered = List.of();
    private String query = "";
    private boolean focused;
    private int page;

    // Layout des letzten Frames
    private float x1, y1, x2, y2;
    private int cols, rows;
    private ItemRef hovered;

    ItemPanel(AioCore core, ItemListModule module) {
        this.core = core;
        this.module = module;
    }

    private void ensureItems() {
        if (all.isEmpty()) {
            all = core.platform().allItems();
            refilter();
        }
    }

    private void refilter() {
        filtered = ItemSearch.filter(all, query);
        page = 0;
    }

    public String query() { return query; }

    public void setQuery(String q) {
        query = q == null ? "" : q;
        refilter();
    }

    public boolean isFocused() { return focused; }

    public ItemRef hovered() { return hovered; }

    /**
     * @param left linker Rand des freien Bereichs (rechts vom Inventar)
     */
    public void render(Renderer r, int mx, int my, float left) {
        ensureItems();
        hovered = null;
        int w = r.screenWidth(), h = r.screenHeight();
        x1 = Math.max(left + 6, w - 6 - module.maxColumns() * CELL);
        x2 = w - 6;
        y1 = 18;
        y2 = h - SEARCH_H - 8;
        cols = Math.max(0, (int) ((x2 - x1) / CELL));
        rows = Math.max(0, (int) ((y2 - y1) / CELL));
        if (cols < 2 || rows < 1) return;
        x1 = x2 - cols * CELL;
        int perPage = cols * rows;
        int pages = Math.max(1, (filtered.size() + perPage - 1) / perPage);
        page = Math.max(0, Math.min(pages - 1, page));

        // Kopf: Seiten
        String pageText = (page + 1) + "/" + pages;
        r.centeredText(pageText, (x1 + x2) / 2, 6, Theme.TEXT_DIM, true);
        r.text("<", x1, 6, page > 0 ? Theme.TEXT : Theme.TEXT_MUTED, true);
        r.text(">", x2 - r.textWidth(">"), 6, page < pages - 1 ? Theme.TEXT : Theme.TEXT_MUTED, true);

        int start = page * perPage;
        for (int i = 0; i < perPage && start + i < filtered.size(); i++) {
            float cx = x1 + (i % cols) * CELL, cy = y1 + (i / cols) * CELL;
            ItemRef it = filtered.get(start + i);
            boolean hover = mx >= cx && mx < cx + CELL && my >= cy && my < cy + CELL;
            if (hover) {
                r.fill(cx, cy, cx + CELL, cy + CELL, 0x60FFFFFF);
                hovered = it;
            }
            r.item(it, cx + 1, cy + 1, false);
        }

        // Suchfeld
        float sy = h - SEARCH_H - 4;
        r.fill(x1, sy, x2, sy + SEARCH_H, focused ? 0xF0202028 : 0xC0101014);
        r.outline(x1, sy, x2, sy + SEARCH_H, 1, focused ? Theme.accent(core) : 0x60FFFFFF);
        String shown = query.isEmpty() && !focused ? "Suchen… (@mod)" : query;
        while (r.textWidth(shown) > x2 - x1 - 10 && shown.length() > 1) shown = shown.substring(1);
        boolean caret = focused && (System.currentTimeMillis() / 500) % 2 == 0;
        r.text(shown + (caret ? "_" : ""), x1 + 4, sy + 4, query.isEmpty() && !focused ? Theme.TEXT_MUTED : Theme.TEXT, false);
        if (!query.isEmpty()) {
            String count = String.valueOf(filtered.size());
            r.text(count, x2 - 4 - r.textWidth(count), sy + 4, Theme.TEXT_MUTED, false);
        }

        if (hovered != null) drawTooltip(r, hovered, mx, my);
    }

    private void drawTooltip(Renderer r, ItemRef it, int mx, int my) {
        String name = it.name();
        String mod = "§9§o" + (it.modName().isEmpty() ? "Minecraft" : it.modName());
        int tw = Math.max(r.textWidth(name), r.textWidth(mod)) + 8;
        float tx = Math.min(mx + 10, r.screenWidth() - tw - 2), ty = Math.max(2, my - 24);
        if (tx + tw > x1 && mx > x1) tx = Math.max(2, mx - tw - 6);
        r.fill(tx, ty, tx + tw, ty + 24, 0xF0100010);
        r.outline(tx, ty, tx + tw, ty + 24, 1, 0x505000FF);
        r.text(name, tx + 4, ty + 3, 0xFFFFFFFF, true);
        r.text(mod, tx + 4, ty + 13, 0xFF5555FF, true);
    }

    private boolean inGrid(double mx, double my) { return mx >= x1 && mx < x2 && my >= y1 && my < y1 + rows * CELL; }

    private boolean inSearch(Renderer r, double mx, double my) {
        float sy = r.screenHeight() - SEARCH_H - 4;
        return mx >= x1 && mx <= x2 && my >= sy && my <= sy + SEARCH_H;
    }

    public boolean mouseClicked(Renderer r, double mx, double my, int button) {
        if (cols < 2) return false;
        if (inSearch(r, mx, my)) {
            focused = true;
            if (button == 1) setQuery("");
            return true;
        }
        focused = false;
        if (my >= 2 && my <= 16 && mx >= x1 && mx <= x2) {
            if (mx < (x1 + x2) / 2) page--;
            else page++;
            return true;
        }
        if (inGrid(mx, my) && hovered != null) {
            module.showRecipes(hovered, button == 1);
            return true;
        }
        return false;
    }

    public boolean mouseScrolled(double mx, double my, double amount) {
        if (cols < 2 || !inGrid(mx, my)) return false;
        page += amount < 0 ? 1 : -1;
        return true;
    }

    /** @return true, wenn die Taste verbraucht wurde (z. B. beim Tippen im Suchfeld). */
    public boolean keyPressed(String key, ItemRef hoveredSlotItem) {
        boolean ctrl = core.platform().isKeyDown(Keys.LEFT_CONTROL) || core.platform().isKeyDown(Keys.RIGHT_CONTROL);
        if (focused) {
            if (Keys.ESCAPE.equals(key) || Keys.ENTER.equals(key)) focused = false;
            else if (Keys.BACKSPACE.equals(key) && !query.isEmpty()) setQuery(query.substring(0, query.length() - 1));
            else if (ctrl && Keys.V.equals(key)) setQuery(query + core.platform().clipboard().replaceAll("[\\r\\n]", ""));
            return true; // alle Tasten gehen ins Suchfeld (sonst schließt z. B. E das Inventar)
        }
        if (ctrl && "key.keyboard.f".equals(key)) {
            focused = true;
            return true;
        }
        ItemRef target = hovered != null ? hovered : hoveredSlotItem;
        if (target != null && !target.isEmpty()) {
            if (Keys.R.equals(key)) { module.showRecipes(target, false); return true; }
            if ("key.keyboard.u".equals(key)) { module.showRecipes(target, true); return true; }
        }
        return false;
    }

    public boolean charTyped(String text) {
        if (!focused || text.isEmpty() || text.codePointAt(0) < 32) return false;
        if (query.length() < 64) setQuery(query + text);
        return true;
    }

    public void onClose() { focused = false; }

    // Für Tests
    List<ItemRef> filtered() { return filtered; }
}
