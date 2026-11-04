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
    private float[] prevBtn = new float[4], nextBtn = new float[4];
    private final java.util.List<Object[]> slotHits = new java.util.ArrayList<>();

    public RecipeScreen(AioCore core, ItemRef item, boolean usages) {
        this.core = core;
        open(item, usages);
    }

    private void open(ItemRef item, boolean usages) {
        if (view != null) history.push(view);
        view = new View(item, usages, core.platform().recipes(item, usages));
        index = 0;
    }

    @Override public String title() { return "Rezepte"; }

    @Override public boolean returnsToPrevious() { return true; }

    @Override public void render(Renderer r, int mx, int my, float delta) {
        slotHits.clear();
        hoveredItem = null;
        int w = r.screenWidth(), h = r.screenHeight();
        r.fill(0, 0, w, h, Theme.OVERLAY);
        int pw = 220, ph = 150;
        int px = (w - pw) / 2, py = (h - ph) / 2;
        r.fill(px, py, px + pw, py + ph, Theme.PANEL);
        Theme.accentBar(r, core, px, py, px + pw, py + 2);
        r.item(view.item, px + 8, py + 6, false);
        String head = (view.usages ? "Verwendung: " : "Rezepte: ") + view.item.name();
        r.text(r.trim(head, pw - 40), px + 28, py + 11, Theme.TEXT, true);

        if (!core.platform().recipesAvailable()) {
            r.text("Rezepte sind nur im Singleplayer", px + 10, py + 50, Theme.TEXT_DIM, false);
            r.text("oder mit Fabric-Rezept-Sync verfügbar.", px + 10, py + 62, Theme.TEXT_DIM, false);
            return;
        }
        List<RecipeView> rs = view.recipes;
        if (rs.isEmpty()) {
            r.text(view.usages ? "Wird in keinem Rezept verwendet." : "Kein Rezept gefunden.", px + 10, py + 50, Theme.TEXT_DIM, false);
            return;
        }
        index = Math.max(0, Math.min(rs.size() - 1, index));
        RecipeView rec = rs.get(index);
        r.text(rec.type(), px + 10, py + 30, Theme.TEXT_DIM, false);

        int gw = Math.max(1, rec.width()), gh = Math.max(1, rec.height());
        float gx = px + 20, gy = py + 44;
        long tick = System.currentTimeMillis() / 1000;
        for (int i = 0; i < gw * gh; i++) {
            float sx = gx + (i % gw) * SLOT, sy = gy + (i / gw) * SLOT;
            drawSlot(r, sx, sy);
            if (i < rec.slots().size()) {
                List<ItemRef> opts = rec.slots().get(i);
                if (!opts.isEmpty()) {
                    ItemRef it = opts.get((int) (tick % opts.size()));
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
        r.item(rec.result(), rx + 1, ry + 1, true);
        slotHits.add(new Object[]{rx, ry, rec.result()});
        if (mx >= rx && mx < rx + SLOT && my >= ry && my < ry + SLOT) hoveredItem = rec.result();
        if (rec.station() != null && !rec.station().isEmpty()) r.item(rec.station(), rx + 30, ry + 1, false);

        String counter = (index + 1) + " / " + rs.size();
        r.centeredText(counter, px + pw / 2f, py + ph - 18, Theme.TEXT_DIM, false);
        prevBtn = new float[]{px + 10, py + ph - 22, px + 34, py + ph - 8};
        nextBtn = new float[]{px + pw - 34, py + ph - 22, px + pw - 10, py + ph - 8};
        button(r, prevBtn, "<", mx, my);
        button(r, nextBtn, ">", mx, my);
        if (!history.isEmpty()) r.text("⌫ zurück", px + pw - 54, py + 30, Theme.TEXT_MUTED, false);

        if (hoveredItem != null) {
            String n = hoveredItem.name();
            float tx = Math.min(mx + 10, w - r.textWidth(n) - 8);
            r.fill(tx - 3, my - 14, tx + r.textWidth(n) + 3, my - 3, 0xF0100010);
            r.text(n, tx, my - 12, 0xFFFFFFFF, true);
        }
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
        if (in(prevBtn, mx, my)) { index--; return true; }
        if (in(nextBtn, mx, my)) { index++; return true; }
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
