package dev.aio.core.modules.inventory;

import dev.aio.core.platform.ContainerView;
import dev.aio.core.platform.ContainerView.Click;
import dev.aio.core.platform.ContainerView.Slot;
import dev.aio.core.platform.ItemRef;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Inventory actions that only use real clicks (as a player would make them).
 * That way it works on every server without a server mod.
 */
public final class InventoryOps {
    private InventoryOps() {}

    /** The "other" area for a slot: chest ↔ player inventory, in the player inventory hotbar ↔ main inventory. */
    public static List<Slot> otherRegion(ContainerView c, Slot from) {
        List<Slot> out = new ArrayList<>();
        for (Slot s : c.slots()) {
            if (c.isPlayerOnly()) {
                if (from.hotbar() && s.playerMain()) out.add(s);
                else if (from.playerMain() && s.hotbar()) out.add(s);
            } else if (s.playerInventory() != from.playerInventory() && (!s.playerInventory() || s.playerMain() || s.hotbar())) {
                out.add(s);
            }
        }
        // in the player inventory fill the main inventory first, then the hotbar
        out.sort(Comparator.comparingInt(s -> s.hotbar() ? 1 : 0));
        return out;
    }

    /** All slots in the same area (for sorting). */
    public static List<Slot> sameRegion(ContainerView c, Slot of) {
        List<Slot> out = new ArrayList<>();
        for (Slot s : c.slots()) {
            if (of.playerInventory()) {
                if (of.playerMain() && s.playerMain()) out.add(s);
                else if (of.hotbar() && s.hotbar()) out.add(s);
            } else if (!s.playerInventory()) {
                out.add(s);
            }
        }
        return out;
    }

    public static Slot slot(ContainerView c, int index) {
        for (Slot s : c.slots()) if (s.index() == index) return s;
        return null;
    }

    /** Moves exactly one item from the slot to the other area. */
    public static boolean pushOne(ContainerView c, int index) {
        Slot src = slot(c, index);
        if (src == null || src.item().isEmpty() || !c.cursor().isEmpty()) return false;
        Slot target = findTarget(otherRegion(c, src), src.item());
        if (target == null) return false;
        c.click(src.index(), 0, Click.PICKUP);
        c.click(target.index(), 1, Click.PICKUP);
        c.click(src.index(), 0, Click.PICKUP);
        return true;
    }

    /** Fetches exactly one matching item from the other area into this slot. */
    public static boolean pullOne(ContainerView c, int index) {
        Slot dst = slot(c, index);
        if (dst == null || dst.item().isEmpty() || !c.cursor().isEmpty()) return false;
        if (dst.item().count() >= dst.item().maxStackSize()) return false;
        Slot source = null;
        for (Slot s : otherRegion(c, dst)) {
            if (s.item().sameItemAs(dst.item())) {
                source = s;
                break;
            }
        }
        if (source == null) return false;
        c.click(source.index(), 0, Click.PICKUP);
        c.click(dst.index(), 1, Click.PICKUP);
        c.click(source.index(), 0, Click.PICKUP);
        return true;
    }

    private static Slot findTarget(List<Slot> region, ItemRef item) {
        for (Slot s : region) if (s.item().sameItemAs(item) && s.item().count() < s.item().maxStackSize()) return s;
        for (Slot s : region) if (s.item().isEmpty()) return s;
        return null;
    }

    /** Sorts an area: merge equal stacks, then order by item. */
    public static boolean sort(ContainerView c, List<Slot> region) {
        if (region.isEmpty() || !c.cursor().isEmpty()) return false;
        merge(c, region);
        order(c, region);
        return true;
    }

    private static void merge(ContainerView c, List<Slot> region) {
        int n = region.size();
        for (int i = 0; i < n; i++) {
            Slot a = fresh(c, region.get(i));
            if (a.item().isEmpty() || a.item().maxStackSize() <= 1) continue;
            for (int j = i + 1; j < n; j++) {
                a = fresh(c, region.get(i));
                if (a.item().count() >= a.item().maxStackSize()) break;
                Slot b = fresh(c, region.get(j));
                if (!b.item().sameItemAs(a.item())) continue;
                c.click(b.index(), 0, Click.PICKUP);
                c.click(a.index(), 0, Click.PICKUP);
                if (!c.cursor().isEmpty()) c.click(b.index(), 0, Click.PICKUP);
            }
        }
    }

    /** Sort key: id, name, then larger stacks first. */
    static final Comparator<ItemRef> ORDER = Comparator
            .comparing(ItemRef::id)
            .thenComparing(ItemRef::name)
            .thenComparing(Comparator.comparingInt(ItemRef::count).reversed());

    private static void order(ContainerView c, List<Slot> region) {
        int n = region.size();
        List<ItemRef> desired = new ArrayList<>();
        for (Slot s : region) {
            ItemRef it = fresh(c, s).item();
            if (!it.isEmpty()) desired.add(it);
        }
        desired.sort(ORDER);
        for (int i = 0; i < desired.size(); i++) {
            ItemRef want = desired.get(i);
            Slot target = fresh(c, region.get(i));
            if (matches(target.item(), want)) continue;
            int j = -1;
            for (int k = i + 1; k < n; k++) {
                if (matches(fresh(c, region.get(k)).item(), want)) {
                    j = k;
                    break;
                }
            }
            if (j < 0) continue;
            Slot from = region.get(j);
            c.click(from.index(), 0, Click.PICKUP);
            c.click(target.index(), 0, Click.PICKUP);
            if (!c.cursor().isEmpty()) c.click(from.index(), 0, Click.PICKUP);
        }
    }

    private static boolean matches(ItemRef a, ItemRef b) {
        return !a.isEmpty() && a.sameItemAs(b) && a.count() == b.count();
    }

    /** Reads the current state of a slot (after previous clicks). */
    private static Slot fresh(ContainerView c, Slot s) {
        Slot now = slot(c, s.index());
        return now != null ? now : s;
    }
}
