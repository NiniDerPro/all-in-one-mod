package dev.aio.core.modules.inventory;

import dev.aio.core.AioCore;
import dev.aio.core.platform.ItemRef;
import dev.aio.core.platform.Keys;
import dev.aio.core.platform.Renderer;
import dev.aio.core.ui.McGui;

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
    private ItemSearch.Index index;
    private List<ItemRef> filtered = List.of();
    private String query = "";
    private boolean focused;
    /** Strg+A: der ganze Suchtext ist markiert, Tippen ersetzt ihn. */
    private boolean allSelected;
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
            index = new ItemSearch.Index(all);
            refilter();
        }
    }

    private void refilter() {
        filtered = index != null ? index.filter(query) : ItemSearch.filter(all, query);
        page = 0;
    }

    public String query() { return query; }

    public void setQuery(String q) {
        query = q == null ? "" : q;
        refilter();
    }

    public boolean isFocused() { return focused; }

    public ItemRef hovered() { return hovered; }

    /** Höhe der Knöpfe oben und des Suchfelds unten (wie Vanilla-Knöpfe). */
    private static final int BAR = 20;
    private float searchY;

    /**
     * Im Stil bekannter Item-Listen: oben Blättern-Knöpfe mit Seitenzahl, darunter das Raster,
     * unten Suchfeld und Einstellungs-Knopf.
     *
     * @param left linker Rand des freien Bereichs (rechts vom Inventar bzw. den Effekten)
     */
    public void render(Renderer r, int mx, int my, float left) {
        ensureItems();
        module.ensureWorldInfo(); // Handel, Beute & Co. schon im Hintergrund laden, bevor R/U gedrückt wird
        hovered = null;
        int w = r.screenWidth(), h = r.screenHeight();
        x1 = Math.max(left + 4, w - 4 - module.maxColumns() * CELL);
        x2 = w - 4;
        y1 = 4 + BAR + 4;
        searchY = h - BAR - 4;
        y2 = searchY - 4;
        cols = Math.max(0, (int) ((x2 - x1) / CELL));
        rows = Math.max(0, (int) ((y2 - y1) / CELL));
        if (cols < 2 || rows < 1) return;
        x1 = x2 - cols * CELL;
        int perPage = cols * rows;
        int pages = Math.max(1, (filtered.size() + perPage - 1) / perPage);
        page = Math.max(0, Math.min(pages - 1, page));

        // Kopf: ◀ 1/16 ▶
        McGui.arrowButton(r, x1, 4, x1 + BAR, 4 + BAR, false, mx, my, pages > 1);
        McGui.arrowButton(r, x2 - BAR, 4, x2, 4 + BAR, true, mx, my, pages > 1);
        r.centeredText((page + 1) + "/" + pages, (x1 + x2) / 2f, 10, 0xFFFFFFFF, true);

        int start = page * perPage;
        for (int i = 0; i < perPage && start + i < filtered.size(); i++) {
            float cx = x1 + (i % cols) * CELL, cy = y1 + (i / cols) * CELL;
            ItemRef it = filtered.get(start + i);
            boolean hover = mx >= cx && mx < cx + CELL && my >= cy && my < cy + CELL;
            if (hover) {
                r.fill(cx, cy, cx + CELL, cy + CELL, 0x80FFFFFF);
                hovered = it;
            }
            r.item(it, cx + 1, cy + 1, false);
        }

        // Suchfeld und Einstellungen
        float fx2 = x2 - BAR - 2;
        McGui.textField(r, x1, searchY, fx2, searchY + BAR, focused);
        String shown = query.isEmpty() && !focused ? "Search… (@mod)" : query;
        float maxW = fx2 - x1 - 10;
        while (r.textWidth(shown) > maxW && shown.length() > 1) shown = shown.substring(1);
        float tx = x1 + 4, ty = searchY + 6;
        if (allSelected && !query.isEmpty()) r.fill(tx - 1, ty - 1, tx + r.textWidth(shown) + 1, ty + 9, 0xFF3355CC);
        boolean caret = focused && !allSelected && (System.currentTimeMillis() / 500) % 2 == 0;
        r.text(shown + (caret ? "_" : ""), tx, ty, query.isEmpty() && !focused ? 0xFF707070 : 0xFFE0E0E0, true);
        if (!query.isEmpty()) {
            String count = String.valueOf(filtered.size());
            r.text(count, fx2 - 4 - r.textWidth(count), ty, 0xFF707070, false);
        }
        boolean wrenchHover = McGui.button(r, x2 - BAR, searchY, x2, searchY + BAR, mx, my, true);
        McGui.wrench(r, x2 - BAR + 4, searchY + 4, wrenchHover ? 0xFFFFFFA0 : 0xFFE0E0E0);

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
        return mx >= x1 && mx <= x2 - BAR - 2 && my >= searchY && my <= searchY + BAR;
    }

    public boolean mouseClicked(Renderer r, double mx, double my, int button) {
        if (cols < 2) return false;
        if (inSearch(r, mx, my)) {
            focused = true;
            allSelected = false;
            if (button == 1) setQuery("");
            return true;
        }
        focused = false;
        allSelected = false;
        if (my >= 4 && my <= 4 + BAR) {
            if (mx >= x1 && mx <= x1 + BAR) { page--; return true; }
            if (mx >= x2 - BAR && mx <= x2) { page++; return true; }
        }
        if (mx >= x2 - BAR && mx <= x2 && my >= searchY && my <= searchY + BAR) {
            core.openClickGui(module); // Einstellungen der Item-Liste
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
            if (Keys.ESCAPE.equals(key) || Keys.ENTER.equals(key)) {
                focused = false;
                allSelected = false;
            } else if (ctrl && "key.keyboard.a".equals(key)) {
                allSelected = !query.isEmpty(); // Strg+A: alles markieren
            } else if (Keys.BACKSPACE.equals(key) || Keys.DELETE.equals(key)) {
                if (allSelected) setQuery("");
                else if (Keys.BACKSPACE.equals(key) && !query.isEmpty()) setQuery(query.substring(0, query.length() - 1));
                allSelected = false;
            } else if (ctrl && Keys.C.equals(key)) {
                core.platform().setClipboard(query);
            } else if (ctrl && Keys.V.equals(key)) {
                String paste = core.platform().clipboard().replaceAll("[\\r\\n]", "");
                setQuery(allSelected ? paste : query + paste);
                allSelected = false;
            }
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
        if (allSelected) {
            setQuery(text); // markierter Text wird ersetzt
            allSelected = false;
        } else if (query.length() < 64) {
            setQuery(query + text);
        }
        return true;
    }

    public void onClose() {
        focused = false;
        allSelected = false;
    }

    // Für Tests
    List<ItemRef> filtered() { return filtered; }
}
