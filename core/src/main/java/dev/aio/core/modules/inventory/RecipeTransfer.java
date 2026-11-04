package dev.aio.core.modules.inventory;

import dev.aio.core.platform.ContainerView;
import dev.aio.core.platform.ContainerView.Click;
import dev.aio.core.platform.ContainerView.Grid;
import dev.aio.core.platform.ContainerView.Slot;
import dev.aio.core.platform.ItemRef;
import dev.aio.core.platform.RecipeView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Legt ein Rezept per echten Klicks in das Eingabe-Raster (wie der "+"-Knopf in JEI/REI).
 *
 * <ul>
 *   <li>Einfach: pro Zutat genau ein Item (höchstens 3 Klicks pro Slot).</li>
 *   <li>Maximal: ganze Stacks per Ziehen-Verteilen auf alle Slots derselben Zutat, bis die Slots
 *       voll sind oder nichts mehr da ist. Das braucht nur wenige Pakete (Server-Spamschutz!).</li>
 * </ul>
 * Vorher wird das Raster geleert; klappt das nicht (Inventar voll), wird abgebrochen.
 */
public final class RecipeTransfer {
    public enum Status { OK, WRONG_STATION, TOO_BIG, MISSING_ITEMS }

    /** Obergrenze an Klickpaketen pro Übertragung, damit kein Server wegen Spam kickt. */
    public static final int MAX_CLICKS = 300;

    private RecipeTransfer() {}

    /** Prüft, ob das Rezept hier passt und genug Zutaten für einmal Herstellen da sind. */
    public static Status check(ContainerView c, RecipeView recipe) {
        Grid g = c == null ? null : c.grid();
        if (g == null || !g.kinds().contains(recipe.kind())) return Status.WRONG_STATION;
        if (recipe.width() > g.width() || recipe.height() > g.height()) return Status.TOO_BIG;
        return missing(c, recipe).isEmpty() ? Status.OK : Status.MISSING_ITEMS;
    }

    /** Indizes der Rezept-Slots, für die im Inventar (inkl. Raster) kein passendes Item liegt. */
    public static List<Integer> missing(ContainerView c, RecipeView recipe) { return missing(c, recipe, true); }

    /**
     * Überträgt das Rezept.
     *
     * @return wie oft das Rezept jetzt hergestellt werden kann (0 = nichts übertragen)
     */
    public static int transfer(ContainerView c, RecipeView recipe, boolean max) {
        if (check(c, recipe) != Status.OK || !c.cursor().isEmpty()) return 0;
        Grid g = c.grid();
        int[] clicks = {0};

        // 1. Raster leeren, bei vollem Inventar abbrechen statt Items zu vertauschen
        for (int idx : g.slots()) {
            Slot s = InventoryOps.slot(c, idx);
            if (s != null && !s.item().isEmpty()) click(c, clicks, idx, 0, Click.QUICK_MOVE);
        }
        for (int idx : g.slots()) {
            Slot s = InventoryOps.slot(c, idx);
            if (s != null && !s.item().isEmpty()) return 0;
        }
        if (!c.cursor().isEmpty() || !missing(c, recipe, false).isEmpty()) return 0;

        return max ? transferMax(c, recipe, g, clicks) : transferOnce(c, recipe, g, clicks);
    }

    private static int transferOnce(ContainerView c, RecipeView recipe, Grid g, int[] clicks) {
        Map<String, Integer> available = availableCounts(c, false);
        for (int i = 0; i < recipe.slots().size(); i++) {
            List<ItemRef> opts = recipe.slots().get(i);
            if (opts.isEmpty()) continue;
            String id = pick(opts, available);
            if (id == null) return 0;
            available.merge(id, -1, Integer::sum);
            Slot src = findSource(c, id);
            if (src == null) return 0;
            int target = targetIndex(recipe, g, i);
            click(c, clicks, src.index(), 0, Click.PICKUP);
            click(c, clicks, target, 1, Click.PICKUP);
            click(c, clicks, src.index(), 0, Click.PICKUP);
        }
        return 1;
    }

    private static int transferMax(ContainerView c, RecipeView recipe, Grid g, int[] clicks) {
        // Zutat pro Slot festlegen (die mit dem größten Vorrat) und Ziel-Slots je Item-ID sammeln
        Map<String, Integer> available = availableCounts(c, false);
        Map<String, List<Integer>> groups = new LinkedHashMap<>();
        for (int i = 0; i < recipe.slots().size(); i++) {
            List<ItemRef> opts = recipe.slots().get(i);
            if (opts.isEmpty()) continue;
            String best = null;
            for (ItemRef o : opts) {
                int n = available.getOrDefault(o.id(), 0);
                if (n > 0 && (best == null || n > available.getOrDefault(best, 0))) best = o.id();
            }
            if (best == null) return 0;
            groups.computeIfAbsent(best, k -> new ArrayList<>()).add(targetIndex(recipe, g, i));
        }

        for (Map.Entry<String, List<Integer>> e : groups.entrySet()) {
            String id = e.getKey();
            List<Integer> targets = e.getValue();
            while (clicks[0] < MAX_CLICKS) {
                Slot src = findSource(c, id);
                if (src == null) break;
                int cap = Math.max(1, src.item().maxStackSize());
                List<Integer> needing = new ArrayList<>();
                for (int t : targets) if (count(c, t) < cap) needing.add(t);
                if (needing.isEmpty()) break;

                click(c, clicks, src.index(), 0, Click.PICKUP);
                int k = c.cursor().count();
                boolean distributed = k >= needing.size();
                if (distributed) {
                    c.dragSplit(toArray(needing));
                    clicks[0] += needing.size() + 2;
                }
                // Rest (oder zu wenig für alle Slots) zurücklegen: alle Slots bleiben gleich voll
                if (!c.cursor().isEmpty()) click(c, clicks, src.index(), 0, Click.PICKUP);
                if (!distributed || !c.cursor().isEmpty()) break;
            }
        }

        int crafts = Integer.MAX_VALUE;
        for (List<Integer> targets : groups.values()) for (int t : targets) crafts = Math.min(crafts, count(c, t));
        return crafts == Integer.MAX_VALUE ? 0 : crafts;
    }

    private static void click(ContainerView c, int[] clicks, int slot, int button, Click type) {
        c.click(slot, button, type);
        clicks[0]++;
    }

    private static int targetIndex(RecipeView recipe, Grid g, int i) {
        return g.slots()[(i / recipe.width()) * g.width() + (i % recipe.width())];
    }

    private static int count(ContainerView c, int index) {
        Slot s = InventoryOps.slot(c, index);
        return s == null || s.item().isEmpty() ? 0 : s.item().count();
    }

    private static int[] toArray(List<Integer> list) {
        int[] a = new int[list.size()];
        for (int i = 0; i < a.length; i++) a[i] = list.get(i);
        return a;
    }

    private static List<Integer> missing(ContainerView c, RecipeView recipe, boolean includeGrid) {
        Map<String, Integer> available = availableCounts(c, includeGrid);
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < recipe.slots().size(); i++) {
            List<ItemRef> opts = recipe.slots().get(i);
            if (opts.isEmpty()) continue;
            String pick = pick(opts, available);
            if (pick == null) out.add(i);
            else available.merge(pick, -1, Integer::sum);
        }
        return out;
    }

    private static String pick(List<ItemRef> opts, Map<String, Integer> available) {
        for (ItemRef o : opts) if (available.getOrDefault(o.id(), 0) > 0) return o.id();
        return null;
    }

    /** Anzahl pro Item-ID im Spielerinventar (optional plus Inhalt des Rasters). */
    private static Map<String, Integer> availableCounts(ContainerView c, boolean includeGrid) {
        Map<String, Integer> m = new HashMap<>();
        for (Slot s : c.slots()) {
            if (!s.item().isEmpty() && (s.playerMain() || s.hotbar())) m.merge(s.item().id(), s.item().count(), Integer::sum);
        }
        Grid g = c.grid();
        if (includeGrid && g != null) {
            for (int idx : g.slots()) {
                Slot s = InventoryOps.slot(c, idx);
                if (s != null && !s.item().isEmpty() && !s.playerInventory()) m.merge(s.item().id(), s.item().count(), Integer::sum);
            }
        }
        return m;
    }

    /** Erster Stack dieser Item-ID im Spielerinventar (Hauptinventar oder Hotbar). */
    private static Slot findSource(ContainerView c, String id) {
        for (Slot s : c.slots()) {
            if (!s.item().isEmpty() && (s.playerMain() || s.hotbar()) && id.equals(s.item().id())) return s;
        }
        return null;
    }
}
