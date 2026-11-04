package dev.aio.core.modules.inventory;

import dev.aio.core.platform.ContainerView;
import dev.aio.core.platform.ContainerView.Click;
import dev.aio.core.platform.ContainerView.Grid;
import dev.aio.core.platform.ContainerView.Slot;
import dev.aio.core.platform.ItemRef;
import dev.aio.core.platform.RecipeView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Legt ein Rezept per echten Klicks in das Eingabe-Raster (wie der "+"-Knopf in JEI/REI):
 * erst das Raster leeren, dann pro Zutat ein Item aus dem Spielerinventar hineinlegen.
 * Mit {@code max} wird so oft wiederholt, wie Zutaten da sind (höchstens bis zur Stapelgröße).
 */
public final class RecipeTransfer {
    public enum Status { OK, WRONG_STATION, TOO_BIG, MISSING_ITEMS }

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

    /** Überträgt das Rezept. @return wie oft es ins Raster gelegt wurde (0 = gar nicht) */
    public static int transfer(ContainerView c, RecipeView recipe, boolean max) {
        if (check(c, recipe) != Status.OK || !c.cursor().isEmpty()) return 0;
        Grid g = c.grid();
        // 1. Raster leeren
        for (int idx : g.slots()) c.click(idx, 0, Click.QUICK_MOVE);
        if (!c.cursor().isEmpty()) return 0;

        int limit = max ? maxRepeats(recipe) : 1;
        int done = 0;
        for (; done < limit; done++) {
            if (!missing(c, recipe, false).isEmpty()) break;
            for (int i = 0; i < recipe.slots().size(); i++) {
                List<ItemRef> opts = recipe.slots().get(i);
                if (opts.isEmpty()) continue;
                int target = g.slots()[(i / recipe.width()) * g.width() + (i % recipe.width())];
                Slot targetSlot = InventoryOps.slot(c, target);
                // liegt schon etwas im Zielslot (vorherige Runde), muss es dasselbe Item sein
                String wanted = targetSlot != null && !targetSlot.item().isEmpty() ? targetSlot.item().id() : null;
                Slot src = findSource(c, opts, wanted);
                if (src == null) return done;
                c.click(src.index(), 0, Click.PICKUP);
                c.click(target, 1, Click.PICKUP);
                c.click(src.index(), 0, Click.PICKUP);
            }
        }
        return done;
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

    /** Wie oft man das Rezept stapeln kann, bevor ein Rasterslot voll ist. */
    private static int maxRepeats(RecipeView recipe) {
        int max = 64;
        for (List<ItemRef> opts : recipe.slots()) {
            for (ItemRef it : opts) max = Math.min(max, Math.max(1, it.maxStackSize()));
        }
        return max;
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

    /**
     * Quelle im Spielerinventar. Liegt im Zielslot schon ein Item, nur genau dieses nehmen
     * (ein anderes würde MC beim Rechtsklick tauschen statt stapeln).
     */
    private static Slot findSource(ContainerView c, List<ItemRef> opts, String requiredId) {
        for (Slot s : c.slots()) {
            if (s.item().isEmpty() || !(s.playerMain() || s.hotbar())) continue;
            for (ItemRef o : opts) {
                if (o.id().equals(s.item().id()) && (requiredId == null || requiredId.equals(o.id()))) return s;
            }
        }
        return null;
    }
}
