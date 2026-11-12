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
 * Puts a recipe into the input grid with real clicks (like the "+" button in JEI/REI).
 *
 * <ul>
 *   <li>Simple: exactly one item per ingredient (at most 3 clicks per slot).</li>
 *   <li>Maximum: whole stacks by drag-distributing over all slots of the same ingredient, until the slots
 *       are full or nothing is left. That needs only a few packets (server spam protection!).</li>
 * </ul>
 * Before that the grid is emptied; if that does not work (inventory full), it aborts.
 */
public final class RecipeTransfer {
    public enum Status { OK, WRONG_STATION, TOO_BIG, MISSING_ITEMS }

    /** Upper limit of click packets per transfer, so that no server kicks for spam. */
    public static final int MAX_CLICKS = 300;

    private RecipeTransfer() {}

    /** Checks whether the recipe fits here and enough ingredients for one craft are there. */
    public static Status check(ContainerView c, RecipeView recipe) {
        Grid g = c == null ? null : c.grid();
        if (g == null || !g.kinds().contains(recipe.kind())) return Status.WRONG_STATION;
        if (recipe.width() > g.width() || recipe.height() > g.height()) return Status.TOO_BIG;
        return missing(c, recipe).isEmpty() ? Status.OK : Status.MISSING_ITEMS;
    }

    /** Indices of the recipe slots for which no matching item lies in the inventory (incl. grid). */
    public static List<Integer> missing(ContainerView c, RecipeView recipe) { return missing(c, recipe, true); }

    /**
     * Transfers the recipe.
     *
     * @return how often the recipe can now be crafted (0 = nothing transferred)
     */
    public static int transfer(ContainerView c, RecipeView recipe, boolean max) {
        if (check(c, recipe) != Status.OK || !c.cursor().isEmpty()) return 0;
        Grid g = c.grid();
        int[] clicks = {0};

        // 1. Empty the grid, abort when the inventory is full instead of swapping items
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
        // Determine the ingredient per slot (the one with the largest supply) and collect target slots per item id
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
                // Put back the rest (or too little for all slots): all slots stay equally full
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

    /** Count per item id in the player inventory (optionally plus the contents of the grid). */
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

    /** First stack of this item id in the player inventory (main inventory or hotbar). */
    private static Slot findSource(ContainerView c, String id) {
        for (Slot s : c.slots()) {
            if (!s.item().isEmpty() && (s.playerMain() || s.hotbar()) && id.equals(s.item().id())) return s;
        }
        return null;
    }
}
