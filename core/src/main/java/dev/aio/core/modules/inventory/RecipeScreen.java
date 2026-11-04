package dev.aio.core.modules.inventory;

import dev.aio.core.AioCore;
import dev.aio.core.platform.ItemRef;
import dev.aio.core.platform.Keys;
import dev.aio.core.platform.RecipeView;
import dev.aio.core.platform.Renderer;
import dev.aio.core.ui.Theme;
import dev.aio.core.ui.UiScreen;

import java.util.ArrayDeque;
import java.util.List;

/**
 * Rezeptansicht: Raster mit Zutaten (Alternativen wechseln jede Sekunde), Pfeil und Ergebnis.
 * Klick auf eine Zutat zeigt deren Rezepte, Rechtsklick deren Verwendungen; Rücktaste geht zurück.
 */
public final class RecipeScreen implements UiScreen {
    private static final int SLOT = 18;

    private record View(ItemRef item, boolean usages, List<RecipeView> recipes) {}

    private final AioCore core;
    private final ArrayDeque<View> history = new ArrayDeque<>();
    private View view;
    private int index;
    private ItemRef hoveredItem;
    private float[] prevBtn = new float[4], nextBtn = new float[4], plusBtn = new float[4];
    private RecipeTransfer.Status transferStatus = RecipeTransfer.Status.WRONG_STATION;
    private RecipeView shownRecipe;
    private final java.util.List<Object[]> slotHits = new java.util.ArrayList<>();

    public RecipeScreen(AioCore core, ItemRef item, boolean usages) {
        this.core = core;
        open(item, usages);
    }

    /** Auswahl je Slot bei Kombinations-Ansichten (Verzierungen). */
    private int[] selection = new int[0];
    private int selectionFor = -1;
    /** Beim Öffnen fehlten noch die Welt-Infos: sobald sie da sind, neu laden. */
    private boolean pendingInfo;
    private final java.util.List<Object[]> comboHits = new java.util.ArrayList<>();

    private List<RecipeView> load(ItemRef item, boolean usages) {
        ItemListModule list = core.module(ItemListModule.class);
        if (list == null) return core.platform().recipes(item, usages);
        pendingInfo = list.worldInfoPending();
        return list.recipesFor(item, usages);
    }

    private void open(ItemRef item, boolean usages) {
        if (view != null) history.push(view);
        view = new View(item, usages, load(item, usages));
        index = 0;
    }

    @Override public String title() { return "Rezepte"; }

    @Override public boolean returnsToPrevious() { return true; }

    @Override public void render(Renderer r, int mx, int my, float delta) {
        slotHits.clear();
        comboHits.clear();
        hoveredItem = null;
        if (pendingInfo) {
            ItemListModule list = core.module(ItemListModule.class);
            if (list == null || !list.worldInfoPending()) view = new View(view.item, view.usages, load(view.item, view.usages));
        }
        int w = r.screenWidth(), h = r.screenHeight();
        r.fill(0, 0, w, h, Theme.OVERLAY);
        int pw = Math.min(w - 10, 250), ph = Math.min(h - 10, 196);
        int px = (w - pw) / 2, py = (h - ph) / 2;
        r.fill(px, py, px + pw, py + ph, Theme.PANEL);
        Theme.accentBar(r, core, px, py, px + pw, py + 2);
        r.item(view.item, px + 8, py + 6, false);
        String head = (view.usages ? "Verwendung: " : "Rezepte: ") + view.item.name();
        r.text(r.trim(head, pw - 90), px + 28, py + 11, Theme.TEXT, true);

        if (!core.platform().recipesAvailable()) {
            r.text("Rezepte sind erst in einer Welt verfügbar.", px + 10, py + 50, Theme.TEXT_DIM, false);
            return;
        }
        List<RecipeView> rs = view.recipes;
        if (rs.isEmpty()) {
            r.text(view.usages ? "Wird in keinem Rezept verwendet." : "Kein Rezept gefunden.", px + 10, py + 50, Theme.TEXT_DIM, false);
            if (pendingInfo) r.text("Handel, Beute & Spawns werden geladen…", px + 10, py + 64, Theme.TEXT_MUTED, false);
            return;
        }
        index = Math.max(0, Math.min(rs.size() - 1, index));
        RecipeView rec = rs.get(index);
        shownRecipe = rec;
        dev.aio.core.platform.ContainerView container = core.platform().container();
        transferStatus = RecipeTransfer.check(container, rec);
        java.util.List<Integer> missing = transferStatus == RecipeTransfer.Status.MISSING_ITEMS
                ? RecipeTransfer.missing(container, rec) : java.util.List.of();
        r.text(rec.type(), px + 10, py + 30, Theme.TEXT_DIM, false);
        if (pendingInfo) r.text("lädt…", px + pw - 40, py + 30, Theme.TEXT_MUTED, false);

        if (rec.isList()) {
            renderList(r, rec, px, py, pw, mx, my);
            renderFooter(r, rs, px, py, pw, ph, mx, my);
            renderTooltip(r, mx, my, w);
            return;
        }
        if (selectionFor != index || selection.length != rec.slots().size()) {
            selection = new int[rec.slots().size()];
            selectionFor = index;
        }

        int gw = Math.max(1, rec.width()), gh = Math.max(1, rec.height());
        float gx = px + 20, gy = py + 44;
        long tick = System.currentTimeMillis() / 1000;
        for (int i = 0; i < gw * gh; i++) {
            float sx = gx + (i % gw) * SLOT, sy = gy + (i / gw) * SLOT;
            drawSlot(r, sx, sy);
            if (missing.contains(i)) r.fill(sx + 1, sy + 1, sx + SLOT - 1, sy + SLOT - 1, 0x80FF3030);
            if (i < rec.slots().size()) {
                List<ItemRef> opts = rec.slots().get(i);
                if (!opts.isEmpty()) {
                    ItemRef it = rec.combos() ? opts.get(Math.floorMod(selection[i], opts.size())) : opts.get((int) (tick % opts.size()));
                    if (rec.combos() && opts.size() > 1) comboHits.add(new Object[]{sx, sy, i});
                    r.item(it, sx + 1, sy + 1, true);
                    slotHits.add(new Object[]{sx, sy, it});
                    if (mx >= sx && mx < sx + SLOT && my >= sy && my < sy + SLOT) hoveredItem = it;
                }
            }
        }
        float ax = gx + gw * SLOT + 10, ay = gy + (gh * SLOT) / 2f - 4;
        r.text("→", ax, ay, Theme.TEXT, true);
        float rx = ax + 18, ry = gy + (gh * SLOT) / 2f - SLOT / 2f;
        drawSlot(r, rx, ry);
        ItemRef result = rec.combos() && !rec.results().isEmpty() ? rec.results().get(comboIndex(rec)) : rec.result();
        r.item(result, rx + 1, ry + 1, true);
        slotHits.add(new Object[]{rx, ry, result});
        if (mx >= rx && mx < rx + SLOT && my >= ry && my < ry + SLOT) hoveredItem = result;
        if (rec.station() != null && !rec.station().isEmpty()) r.item(rec.station(), rx + 30, ry + 1, false);

        float ny = gy + gh * SLOT + 8;
        for (String note : rec.notes()) {
            r.text(r.trim(note, pw - 20), px + 10, ny, Theme.TEXT_DIM, false);
            ny += 10;
        }
        if (rec.combos()) r.text("Klick auf Rüstung/Material: wechseln · Shift+Klick: Rezepte", px + 10, ny + 2, Theme.TEXT_MUTED, false);

        if (rec.kind() == dev.aio.core.platform.RecipeKind.OTHER) {
            plusBtn = new float[4];
            renderFooter(r, rs, px, py, pw, ph, mx, my);
            renderTooltip(r, mx, my, w);
            return;
        }

        // "+": ins offene Inventar übertragen
        plusBtn = new float[]{px + pw - 30, py + 44, px + pw - 10, py + 60};
        boolean ok = transferStatus == RecipeTransfer.Status.OK;
        boolean hoverPlus = mx >= plusBtn[0] && mx <= plusBtn[2] && my >= plusBtn[1] && my <= plusBtn[3];
        int plusColor = ok ? (hoverPlus ? Theme.accent(core) : 0xFF2E7D32)
                : transferStatus == RecipeTransfer.Status.MISSING_ITEMS ? 0xFF8A3030 : 0xFF3A3A44;
        r.fill(plusBtn[0], plusBtn[1], plusBtn[2], plusBtn[3], plusColor);
        r.centeredText("+", (plusBtn[0] + plusBtn[2]) / 2, plusBtn[1] + 4, ok ? Theme.TEXT : Theme.TEXT_MUTED, true);
        if (hoverPlus) {
            String tip = switch (transferStatus) {
                case OK -> "Klick: einmal  ·  Shift: so viele wie möglich";
                case MISSING_ITEMS -> "Es fehlen Zutaten (rot markiert)";
                case TOO_BIG -> "Passt nicht ins 2×2 – Werkbank öffnen";
                case WRONG_STATION -> stationHint(rec.kind());
            };
            float tw = r.textWidth(tip);
            float tx = Math.max(2, Math.min(mx - tw / 2, w - tw - 4));
            r.fill(tx - 3, plusBtn[3] + 2, tx + tw + 3, plusBtn[3] + 13, 0xF0100010);
            r.text(tip, tx, plusBtn[3] + 4, 0xFFFFFFFF, false);
        }

        renderFooter(r, rs, px, py, pw, ph, mx, my);
        renderTooltip(r, mx, my, w);
    }

    /** Liste aus Symbol und Text (Beute, Spawns, Berufe, …). */
    private void renderList(Renderer r, RecipeView rec, int px, int py, int pw, int mx, int my) {
        float y = py + 44;
        for (String note : rec.notes()) {
            r.text(r.trim(note, pw - 20), px + 10, y, Theme.TEXT_DIM, false);
            y += 10;
        }
        if (!rec.notes().isEmpty()) y += 2;
        for (RecipeView.Row row : rec.rows()) {
            float ix = px + 10;
            drawSlot(r, ix, y);
            if (row.icon() != null && !row.icon().isEmpty()) {
                r.item(row.icon(), ix + 1, y + 1, false);
                if (mx >= ix && mx < ix + SLOT && my >= y && my < y + SLOT) hoveredItem = row.icon();
            }
            r.text(r.trim(row.label(), pw - 40), ix + SLOT + 5, y + 5, Theme.TEXT, false);
            y += SLOT + 1;
        }
    }

    private void renderFooter(Renderer r, List<RecipeView> rs, int px, int py, int pw, int ph, int mx, int my) {
        String counter = (index + 1) + " / " + rs.size();
        r.centeredText(counter, px + pw / 2f, py + ph - 18, Theme.TEXT_DIM, false);
        prevBtn = new float[]{px + 10, py + ph - 22, px + 34, py + ph - 8};
        nextBtn = new float[]{px + pw - 34, py + ph - 22, px + pw - 10, py + ph - 8};
        button(r, prevBtn, "<", mx, my);
        button(r, nextBtn, ">", mx, my);
        if (!history.isEmpty()) r.text("⌫ zurück", px + pw - 54, py + 18, Theme.TEXT_MUTED, false);
    }

    private void renderTooltip(Renderer r, int mx, int my, int w) {
        if (hoveredItem == null) return;
        String n = hoveredItem.name();
        float tx = Math.min(mx + 10, w - r.textWidth(n) - 8);
        r.fill(tx - 3, my - 14, tx + r.textWidth(n) + 3, my - 3, 0xF0100010);
        r.text(n, tx, my - 12, 0xFFFFFFFF, true);
    }

    /** Ergebnis-Index aus der Auswahl: gemischte Basis über alle Slots mit mehreren Möglichkeiten. */
    private int comboIndex(RecipeView rec) {
        int idx = 0;
        for (int i = 0; i < rec.slots().size(); i++) {
            int n = rec.slots().get(i).size();
            if (n > 1) idx = idx * n + Math.floorMod(selection[i], n);
        }
        return Math.min(Math.max(0, idx), rec.results().size() - 1);
    }

    private static String stationHint(dev.aio.core.platform.RecipeKind kind) {
        return switch (kind) {
            case CRAFTING -> "Inventar oder Werkbank öffnen";
            case FURNACE -> "Ofen öffnen";
            case BLAST_FURNACE -> "Schmelzofen öffnen";
            case SMOKER -> "Räucherofen öffnen";
            case STONECUTTER -> "Steinsäge öffnen";
            default -> "Kann nicht übertragen werden";
        };
    }

    private void drawSlot(Renderer r, float x, float y) {
        r.fill(x, y, x + SLOT, y + SLOT, 0xFF8B8B8B);
        r.fill(x, y, x + SLOT - 1, y + 1, 0xFF373737);
        r.fill(x, y, x + 1, y + SLOT - 1, 0xFF373737);
        r.fill(x + 1, y + SLOT - 1, x + SLOT, y + SLOT, 0xFFFFFFFF);
        r.fill(x + SLOT - 1, y + 1, x + SLOT, y + SLOT, 0xFFFFFFFF);
    }

    private void button(Renderer r, float[] b, String label, int mx, int my) {
        boolean hover = mx >= b[0] && mx <= b[2] && my >= b[1] && my <= b[3];
        r.fill(b[0], b[1], b[2], b[3], hover ? Theme.accent(core) : 0xFF24242C);
        r.centeredText(label, (b[0] + b[2]) / 2, b[1] + 3, Theme.TEXT, false);
    }

    private static boolean in(float[] b, double x, double y) { return x >= b[0] && x <= b[2] && y >= b[1] && y <= b[3]; }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (in(plusBtn, mx, my) && shownRecipe != null) {
            if (transferStatus == RecipeTransfer.Status.OK) {
                boolean shift = core.platform().isKeyDown(Keys.LEFT_SHIFT) || core.platform().isKeyDown(Keys.RIGHT_SHIFT);
                if (RecipeTransfer.transfer(core.platform().container(), shownRecipe, shift) > 0) core.platform().closeScreen();
            }
            return true;
        }
        if (in(prevBtn, mx, my)) { index--; return true; }
        if (in(nextBtn, mx, my)) { index++; return true; }
        boolean shiftDown = core.platform().isKeyDown(Keys.LEFT_SHIFT) || core.platform().isKeyDown(Keys.RIGHT_SHIFT);
        if (!shiftDown) {
            for (Object[] hit : comboHits) {
                float sx = (float) hit[0], sy = (float) hit[1];
                if (mx >= sx && mx < sx + SLOT && my >= sy && my < sy + SLOT) {
                    selection[(int) hit[2]] += button == 1 ? -1 : 1;
                    return true;
                }
            }
        }
        if (hoveredItem != null && !hoveredItem.isEmpty()) {
            open(hoveredItem, button == 1);
            return true;
        }
        return false;
    }

    @Override public boolean mouseScrolled(double x, double y, double amount) {
        index += amount < 0 ? 1 : -1;
        return true;
    }

    @Override public boolean keyPressed(String key) {
        if (Keys.BACKSPACE.equals(key) && !history.isEmpty()) {
            view = history.pop();
            index = 0;
            return true;
        }
        if (hoveredItem != null) {
            if (Keys.R.equals(key)) { open(hoveredItem, false); return true; }
            if ("key.keyboard.u".equals(key)) { open(hoveredItem, true); return true; }
        }
        if ("key.keyboard.left".equals(key)) { index--; return true; }
        if ("key.keyboard.right".equals(key)) { index++; return true; }
        return false;
    }
}
