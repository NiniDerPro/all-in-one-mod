package dev.aio.core.modules.inventory;

import dev.aio.core.AioCore;
import dev.aio.core.platform.ItemRef;
import dev.aio.core.platform.Keys;
import dev.aio.core.platform.RecipeKind;
import dev.aio.core.platform.RecipeView;
import dev.aio.core.platform.Renderer;
import dev.aio.core.ui.McGui;
import dev.aio.core.ui.UiScreen;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rezeptansicht im Stil bekannter Item-Listen: oben Reiter je Kategorie (Werkbank, Ofen, Handel,
 * Beute, …), links die Stationen, darunter Kategorie- und Seitenleiste mit Pfeilen und so viele
 * Rezepte pro Seite, wie hineinpassen. Jedes Rezept hat seinen Kasten und, wo möglich, einen
 * "+"-Knopf zum Übertragen.
 *
 * <p>Klick auf eine Zutat: deren Rezepte, Rechtsklick: Verwendungen; Rücktaste geht zurück.
 */
public final class RecipeScreen implements UiScreen {
    private static final int SLOT = 18;
    private static final int W = 196;
    private static final int TAB_W = 26, TAB_H = 24;
    private static final int ROW_H = 13;
    private static final int LINE = 10;
    private static final int BOX_PAD = 5;

    private record View(ItemRef item, boolean usages, List<RecipeView> recipes) {}

    private final AioCore core;
    private final ArrayDeque<View> history = new ArrayDeque<>();
    private View view;
    /** Rezepte gruppiert nach Kategorie (Reihenfolge wie geliefert). */
    private List<String> categories = List.of();
    private Map<String, List<RecipeView>> byCategory = Map.of();
    private int category;
    private int page;
    /** Beim Öffnen fehlten noch die Welt-Infos: sobald sie da sind, neu laden. */
    private boolean pendingInfo;

    // Klickflächen des letzten Frames
    private record Hit(float x1, float y1, float x2, float y2, Runnable action) {
        boolean in(double x, double y) { return x >= x1 && x < x2 && y >= y1 && y < y2; }
    }
    private record ItemHit(float x, float y, int size, ItemRef item) {}
    private record ComboHit(float x, float y, RecipeView recipe, int slot) {}
    private final List<Hit> hits = new ArrayList<>();
    private final List<ItemHit> itemHits = new ArrayList<>();
    private final List<ComboHit> comboHits = new ArrayList<>();
    /** Auswahl je Slot bei Kombinations-Ansichten (Verzierungen), pro Rezept. */
    private final Map<RecipeView, int[]> selections = new java.util.IdentityHashMap<>();
    private ItemRef hoveredItem;
    private String tooltip;
    private int pageCount = 1;

    public RecipeScreen(AioCore core, ItemRef item, boolean usages) {
        this.core = core;
        open(item, usages);
    }

    private List<RecipeView> load(ItemRef item, boolean usages) {
        ItemListModule list = core.module(ItemListModule.class);
        if (list == null) return core.platform().recipes(item, usages);
        pendingInfo = list.worldInfoPending();
        return list.recipesFor(item, usages);
    }

    private void open(ItemRef item, boolean usages) {
        if (view != null) history.push(view);
        setView(new View(item, usages, load(item, usages)), true);
    }

    private void setView(View v, boolean resetPosition) {
        String oldCategory = categories.isEmpty() ? null : categories.get(Math.min(category, categories.size() - 1));
        view = v;
        Map<String, List<RecipeView>> groups = new LinkedHashMap<>();
        for (RecipeView r : v.recipes) groups.computeIfAbsent(r.type(), k -> new ArrayList<>()).add(r);
        byCategory = groups;
        categories = new ArrayList<>(groups.keySet());
        if (resetPosition || oldCategory == null || !categories.contains(oldCategory)) {
            category = 0;
            page = 0;
        } else {
            category = categories.indexOf(oldCategory);
        }
    }

    @Override public String title() { return "Rezepte"; }

    @Override public boolean returnsToPrevious() { return true; }

    // =====================================================================
    // Zeichnen
    // =====================================================================

    @Override public void render(Renderer r, int mx, int my, float delta) {
        hits.clear();
        itemHits.clear();
        comboHits.clear();
        hoveredItem = null;
        tooltip = null;
        if (pendingInfo) {
            ItemListModule list = core.module(ItemListModule.class);
            if (list == null || !list.worldInfoPending()) setView(new View(view.item, view.usages, load(view.item, view.usages)), false);
        }
        int sw = r.screenWidth(), sh = r.screenHeight();
        r.fill(0, 0, sw, sh, 0x80000000);

        float px = (sw - W) / 2f;
        float maxH = sh - TAB_H - 16;
        List<RecipeView> recs = categories.isEmpty() ? List.of() : byCategory.get(categories.get(category));
        float boxW = W - 16;

        // Seiten bilden: so viele Rezepte, wie in die Höhe passen
        float headerH = 8 + ROW_H * 2 + 4 + 6;
        float avail = maxH - headerH - 8;
        List<List<RecipeView>> pages = new ArrayList<>();
        List<RecipeView> cur = new ArrayList<>();
        float used = 0;
        for (RecipeView rec : recs) {
            float hgt = boxHeight(r, rec) + 4;
            if (!cur.isEmpty() && used + hgt > avail) {
                pages.add(cur);
                cur = new ArrayList<>();
                used = 0;
            }
            cur.add(rec);
            used += hgt;
        }
        if (!cur.isEmpty()) pages.add(cur);
        pageCount = Math.max(1, pages.size());
        page = Math.max(0, Math.min(pageCount - 1, page));
        List<RecipeView> shown = pages.isEmpty() ? List.of() : pages.get(page);
        float contentH = 0;
        for (RecipeView rec : shown) contentH += boxHeight(r, rec) + 4;
        float ph = Math.max(110, headerH + contentH + 6);
        float py = Math.max(TAB_H + 4, (sh - ph) / 2f + TAB_H / 2f);

        renderTabs(r, px, py, mx, my);
        McGui.panel(r, px, py, px + W, py + ph);
        renderCatalysts(r, recs, px, py, mx, my);

        // Kopf: Kategorie und Seite
        float hx1 = px + 8, hx2 = px + W - 8;
        String catName = categories.isEmpty() ? (view.usages ? "Verwendungen" : "Rezepte") : categories.get(category);
        headerRow(r, hx1, py + 8, hx2, catName, categories.size() > 1, mx, my,
                () -> { category = Math.floorMod(category - 1, Math.max(1, categories.size())); page = 0; },
                () -> { category = (category + 1) % Math.max(1, categories.size()); page = 0; });
        headerRow(r, hx1, py + 8 + ROW_H + 2, hx2, (page + 1) + "/" + pageCount, pageCount > 1, mx, my,
                () -> page = Math.floorMod(page - 1, pageCount), () -> page = (page + 1) % pageCount);

        float y = py + headerH;
        if (!core.platform().recipesAvailable()) {
            r.centeredText("Rezepte sind erst in einer Welt verfügbar.", px + W / 2f, y + 10, McGui.TEXT_DARK, false);
        } else if (recs.isEmpty()) {
            r.centeredText(view.usages ? "Wird nirgends verwendet." : "Kein Rezept gefunden.", px + W / 2f, y + 10, McGui.TEXT_DARK, false);
            if (pendingInfo) r.centeredText("Handel, Beute & Spawns werden geladen…", px + W / 2f, y + 24, 0xFF606060, false);
        }
        for (RecipeView rec : shown) {
            float bh = boxHeight(r, rec);
            renderRecipe(r, rec, px + 8, y, boxW, bh, mx, my);
            y += bh + 4;
        }

        // Kopfzeile über dem Fenster: welches Item, zurück
        String head = (view.usages ? "Verwendung: " : "Rezepte: ") + view.item.name();
        r.text(r.trim(head, W - 60), px + 2, py - TAB_H - 12, 0xFFFFFFFF, true);
        if (!history.isEmpty()) {
            String back = "⌫ zurück";
            r.text(back, px + W - r.textWidth(back), py - TAB_H - 12, 0xFFB0B0B0, true);
        }
        if (pendingInfo && !recs.isEmpty()) r.text("lädt…", px + W - 30, py + ph + 3, 0xFFB0B0B0, true);

        renderTooltip(r, mx, my);
    }

    /** Reiter oben: je Kategorie das Symbol ihrer Station (bzw. des ersten Ergebnisses). */
    private void renderTabs(Renderer r, float px, float py, int mx, int my) {
        int max = Math.max(1, W / TAB_W);
        int first = Math.max(0, Math.min(category - max / 2, categories.size() - max));
        for (int i = first; i < Math.min(categories.size(), first + max); i++) {
            float tx = px + (i - first) * TAB_W;
            boolean sel = i == category;
            float ty1 = py - TAB_H + (sel ? 0 : 3);
            McGui.panel(r, tx, ty1, tx + TAB_W - 1, py + (sel ? 3 : 0));
            if (sel) r.fill(tx + 3, py - 1, tx + TAB_W - 4, py + 3, McGui.BG); // fließt ins Fenster über
            ItemRef icon = tabIcon(categories.get(i));
            if (icon != null && !icon.isEmpty()) r.item(icon, tx + (TAB_W - 1 - 16) / 2f, ty1 + 4, false);
            final int idx = i;
            hits.add(new Hit(tx, ty1, tx + TAB_W - 1, py, () -> { category = idx; page = 0; }));
            if (mx >= tx && mx < tx + TAB_W - 1 && my >= ty1 && my < py) tooltip = categories.get(i) + " (" + byCategory.get(categories.get(i)).size() + ")";
        }
    }

    private ItemRef tabIcon(String cat) {
        for (RecipeView rec : byCategory.getOrDefault(cat, List.of())) {
            if (rec.station() != null && !rec.station().isEmpty()) return rec.station();
        }
        for (RecipeView rec : byCategory.getOrDefault(cat, List.of())) {
            if (rec.result() != null && !rec.result().isEmpty()) return rec.result();
        }
        return null;
    }

    /** Links am Fenster: die Stationen der Kategorie (Werkbank, Ofen, …). */
    private void renderCatalysts(Renderer r, List<RecipeView> recs, float px, float py, int mx, int my) {
        List<ItemRef> stations = new ArrayList<>();
        for (RecipeView rec : recs) {
            ItemRef s = rec.station();
            if (s == null || s.isEmpty()) continue;
            boolean dup = false;
            for (ItemRef o : stations) if (o.sameItemAs(s)) { dup = true; break; }
            if (!dup) stations.add(s);
            if (stations.size() >= 6) break;
        }
        if (stations.isEmpty()) return;
        float x2 = px + 2, x1 = x2 - 26, y1 = py + 4;
        McGui.panel(r, x1, y1, x2 + 2, y1 + 8 + stations.size() * SLOT);
        for (int i = 0; i < stations.size(); i++) {
            float sx = x1 + 4, sy = y1 + 4 + i * SLOT;
            McGui.slot(r, sx, sy);
            r.item(stations.get(i), sx + 1, sy + 1, false);
            itemHits.add(new ItemHit(sx, sy, SLOT, stations.get(i)));
            if (mx >= sx && mx < sx + SLOT && my >= sy && my < sy + SLOT) hoveredItem = stations.get(i);
        }
    }

    private void headerRow(Renderer r, float x1, float y, float x2, String label, boolean enabled, int mx, int my,
                           Runnable prev, Runnable next) {
        McGui.arrowButton(r, x1, y, x1 + ROW_H, y + ROW_H, false, mx, my, enabled);
        McGui.arrowButton(r, x2 - ROW_H, y, x2, y + ROW_H, true, mx, my, enabled);
        McGui.inset(r, x1 + ROW_H + 2, y, x2 - ROW_H - 2, y + ROW_H);
        r.centeredText(r.trim(label, x2 - x1 - 2 * ROW_H - 10), (x1 + x2) / 2f, y + 3, 0xFFFFFFFF, true);
        if (enabled) {
            hits.add(new Hit(x1, y, x1 + ROW_H, y + ROW_H, prev));
            hits.add(new Hit(x2 - ROW_H, y, x2, y + ROW_H, next));
        }
    }

    /** Höhe des Kastens eines Rezepts. */
    private float boxHeight(Renderer r, RecipeView rec) {
        float notes = rec.notes().size() * LINE + (rec.combos() ? LINE : 0);
        if (rec.isList()) return BOX_PAD * 2 + notes + (notes > 0 ? 2 : 0) + rec.rows().size() * (SLOT + 1);
        int gh = Math.max(1, rec.height());
        float grid = Math.max(gh * SLOT, 26);
        return BOX_PAD * 2 + grid + (notes > 0 ? notes + 4 : 0);
    }

    private void renderRecipe(Renderer r, RecipeView rec, float x, float y, float w, float h, int mx, int my) {
        McGui.box(r, x, y, x + w, y + h);
        if (rec.isList()) {
            renderList(r, rec, x + BOX_PAD, y + BOX_PAD, w - 2 * BOX_PAD, mx, my);
            return;
        }
        int[] sel = rec.combos() ? selections.computeIfAbsent(rec, k -> new int[rec.slots().size()]) : null;
        int gw = Math.max(1, rec.width()), gh = Math.max(1, rec.height());
        float gridH = Math.max(gh * SLOT, 26);
        float gx = x + BOX_PAD + 6, gy = y + BOX_PAD + (gridH - gh * SLOT) / 2f;
        long tick = System.currentTimeMillis() / 1000;
        boolean freeze = core.platform().isKeyDown(Keys.LEFT_SHIFT) || core.platform().isKeyDown(Keys.RIGHT_SHIFT);
        if (freeze) tick = 0; // Shift hält wechselnde Zutaten an

        // fehlende Zutaten für "+" rot markieren
        dev.aio.core.platform.ContainerView container = core.platform().container();
        boolean transferable = rec.kind() != RecipeKind.OTHER;
        RecipeTransfer.Status status = transferable ? RecipeTransfer.check(container, rec) : RecipeTransfer.Status.WRONG_STATION;
        List<Integer> missing = status == RecipeTransfer.Status.MISSING_ITEMS ? RecipeTransfer.missing(container, rec) : List.of();

        for (int i = 0; i < gw * gh; i++) {
            float sx = gx + (i % gw) * SLOT, sy = gy + (i / gw) * SLOT;
            McGui.slot(r, sx, sy);
            if (missing.contains(i)) r.fill(sx + 1, sy + 1, sx + SLOT - 1, sy + SLOT - 1, 0x80FF3030);
            if (i >= rec.slots().size()) continue;
            List<ItemRef> opts = rec.slots().get(i);
            if (opts.isEmpty()) continue;
            ItemRef it = sel != null ? opts.get(Math.floorMod(sel[i], opts.size())) : opts.get((int) (tick % opts.size()));
            r.item(it, sx + 1, sy + 1, true);
            itemHits.add(new ItemHit(sx, sy, SLOT, it));
            if (sel != null && opts.size() > 1) comboHits.add(new ComboHit(sx, sy, rec, i));
            if (mx >= sx && mx < sx + SLOT && my >= sy && my < sy + SLOT) hoveredItem = it;
        }

        float ax = gx + gw * SLOT + 8, ay = y + BOX_PAD + gridH / 2f - 7;
        McGui.recipeArrow(r, ax, ay);
        float rx = ax + 30, ry = y + BOX_PAD + gridH / 2f - 13;
        McGui.slot(r, rx, ry, 26);
        ItemRef result = sel != null && !rec.results().isEmpty() ? rec.results().get(comboIndex(rec, sel)) : rec.result();
        if (result != null && !result.isEmpty()) {
            r.item(result, rx + 5, ry + 5, true);
            itemHits.add(new ItemHit(rx, ry, 26, result));
            if (mx >= rx && mx < rx + 26 && my >= ry && my < ry + 26) hoveredItem = result;
        }

        // Textzeilen (Stufe, Preis, Chance, …)
        float ny = y + BOX_PAD + gridH + 4;
        for (String note : rec.notes()) {
            r.text(r.trim(note, w - 2 * BOX_PAD), x + BOX_PAD, ny, McGui.TEXT_DARK, false);
            ny += LINE;
        }
        if (rec.combos()) r.text(r.trim("Klick: Rüstung/Material wechseln", w - 2 * BOX_PAD), x + BOX_PAD, ny, 0xFF606060, false);

        // "+" unten rechts (Übertragen ins offene Inventar)
        if (transferable) {
            float bx2 = x + w - 3, by2 = y + h - 3, bx1 = bx2 - 12, by1 = by2 - 12;
            boolean ok = status == RecipeTransfer.Status.OK;
            McGui.button(r, bx1, by1, bx2, by2, mx, my, ok);
            r.centeredText("+", (bx1 + bx2) / 2f + 0.5f, by1 + 2, ok ? 0xFFFFFFFF : 0xFFA0A0A0, ok);
            if (mx >= bx1 && mx < bx2 && my >= by1 && my < by2) tooltip = switch (status) {
                case OK -> "Übertragen · Shift: so viele wie möglich";
                case MISSING_ITEMS -> "Es fehlen Zutaten (rot markiert)";
                case TOO_BIG -> "Passt nicht ins 2×2 – Werkbank öffnen";
                case WRONG_STATION -> stationHint(rec.kind());
            };
            if (ok) hits.add(new Hit(bx1, by1, bx2, by2, () -> {
                boolean shift = core.platform().isKeyDown(Keys.LEFT_SHIFT) || core.platform().isKeyDown(Keys.RIGHT_SHIFT);
                if (RecipeTransfer.transfer(core.platform().container(), rec, shift) > 0) core.platform().closeScreen();
            }));
        }
    }

    /** Liste aus Symbol und Text (Beute, Spawns, Berufe, …). */
    private void renderList(Renderer r, RecipeView rec, float x, float y, float w, int mx, int my) {
        for (String note : rec.notes()) {
            r.text(r.trim(note, w), x, y, McGui.TEXT_DARK, false);
            y += LINE;
        }
        if (!rec.notes().isEmpty()) y += 2;
        for (RecipeView.Row row : rec.rows()) {
            McGui.slot(r, x, y);
            if (row.icon() != null && !row.icon().isEmpty()) {
                r.item(row.icon(), x + 1, y + 1, false);
                itemHits.add(new ItemHit(x, y, SLOT, row.icon()));
                if (mx >= x && mx < x + SLOT && my >= y && my < y + SLOT) hoveredItem = row.icon();
            }
            r.text(r.trim(row.label(), w - SLOT - 6), x + SLOT + 4, y + 5, McGui.TEXT_DARK, false);
            y += SLOT + 1;
        }
    }

    private void renderTooltip(Renderer r, int mx, int my) {
        String text = hoveredItem != null ? hoveredItem.name() : tooltip;
        if (text == null || text.isEmpty()) return;
        float tw = r.textWidth(text);
        float tx = Math.min(mx + 10, r.screenWidth() - tw - 8), ty = Math.max(2, my - 14);
        r.fill(tx - 3, ty - 3, tx + tw + 3, ty + 11, 0xF0100010);
        r.outline(tx - 3, ty - 3, tx + tw + 3, ty + 11, 1, 0x505000FF);
        r.text(text, tx, ty, hoveredItem != null ? hoveredItem.nameColor() : 0xFFFFFFFF, true);
    }

    private static String stationHint(RecipeKind kind) {
        return switch (kind) {
            case CRAFTING -> "Inventar oder Werkbank öffnen";
            case FURNACE -> "Ofen öffnen";
            case BLAST_FURNACE -> "Schmelzofen öffnen";
            case SMOKER -> "Räucherofen öffnen";
            case STONECUTTER -> "Steinsäge öffnen";
            default -> "Kann nicht übertragen werden";
        };
    }

    /** Ergebnis-Index aus der Auswahl: gemischte Basis über alle Slots mit mehreren Möglichkeiten. */
    private static int comboIndex(RecipeView rec, int[] sel) {
        int idx = 0;
        for (int i = 0; i < rec.slots().size(); i++) {
            int n = rec.slots().get(i).size();
            if (n > 1) idx = idx * n + Math.floorMod(sel[i], n);
        }
        return Math.min(Math.max(0, idx), rec.results().size() - 1);
    }

    // =====================================================================
    // Eingabe
    // =====================================================================

    @Override public boolean mouseClicked(double mx, double my, int button) {
        for (Hit h : hits) {
            if (h.in(mx, my)) {
                h.action.run();
                return true;
            }
        }
        boolean shift = core.platform().isKeyDown(Keys.LEFT_SHIFT) || core.platform().isKeyDown(Keys.RIGHT_SHIFT);
        if (!shift) {
            for (ComboHit c : comboHits) {
                if (mx >= c.x && mx < c.x + SLOT && my >= c.y && my < c.y + SLOT) {
                    int[] sel = selections.get(c.recipe);
                    if (sel != null) sel[c.slot] += button == 1 ? -1 : 1;
                    return true;
                }
            }
        }
        for (ItemHit it : itemHits) {
            if (mx >= it.x && mx < it.x + it.size && my >= it.y && my < it.y + it.size && !it.item.isEmpty()) {
                open(it.item, button == 1);
                return true;
            }
        }
        return false;
    }

    @Override public boolean mouseScrolled(double x, double y, double amount) {
        page = Math.floorMod(page + (amount < 0 ? 1 : -1), Math.max(1, pageCount));
        return true;
    }

    @Override public boolean keyPressed(String key) {
        if (Keys.BACKSPACE.equals(key) && !history.isEmpty()) {
            setView(history.pop(), true);
            return true;
        }
        if (hoveredItem != null) {
            if (Keys.R.equals(key)) { open(hoveredItem, false); return true; }
            if ("key.keyboard.u".equals(key)) { open(hoveredItem, true); return true; }
        }
        if ("key.keyboard.left".equals(key)) { page = Math.floorMod(page - 1, Math.max(1, pageCount)); return true; }
        if ("key.keyboard.right".equals(key)) { page = (page + 1) % Math.max(1, pageCount); return true; }
        if ("key.keyboard.up".equals(key) && !categories.isEmpty()) { category = Math.floorMod(category - 1, categories.size()); page = 0; return true; }
        if ("key.keyboard.down".equals(key) && !categories.isEmpty()) { category = (category + 1) % categories.size(); page = 0; return true; }
        return false;
    }
}
