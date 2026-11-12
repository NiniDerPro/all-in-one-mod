package dev.aio.core.modules.inventory;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.platform.ContainerView;
import dev.aio.core.platform.ContainerView.Click;
import dev.aio.core.platform.ContainerView.Slot;
import dev.aio.core.setting.BooleanSetting;

import java.util.HashSet;
import java.util.Set;

/**
 * Maus-Tweaks: Mausrad verschiebt einzelne Items zwischen Inventaren, Shift + Ziehen
 * moves all stacks it is dragged over, middle click sorts.
 */
public final class InventoryTweaksModule extends Module {
    private final BooleanSetting wheel;
    private final BooleanSetting invertWheel;
    private final BooleanSetting shiftDrag;
    private final BooleanSetting middleSort;
    private final Set<Integer> dragged = new HashSet<>();

    public InventoryTweaksModule() {
        super("invtweaks", "Mouse Tweaks", "Scroll to move items, Shift-drag, middle-click to sort", Category.QOL, false);
        wheel = add(new BooleanSetting("wheel", "Scroll moves items", true));
        invertWheel = add(new BooleanSetting("invertWheel", "Invert scroll", false));
        shiftDrag = add(new BooleanSetting("shiftDrag", "Shift + drag moves", true));
        middleSort = add(new BooleanSetting("middleSort", "Middle-click sorts", true));
    }


    /** Mods with the same inventory gestures: do nothing here then, otherwise everything happens twice. */
    private static final String[] CONFLICTS = {"mousetweaks", "mousewheelie", "inventoryprofilesnext"};

    private boolean conflict() {
        for (String id : CONFLICTS) if (platform().isModLoaded(id)) return true;
        return false;
    }

    /** Mouse wheel over a slot. Down = move one item over, up = fetch one back; with Shift the whole stack. */
    public boolean onScroll(ContainerView c, int slotIndex, double amount, boolean shift) {
        if (!isActive() || !wheel.on() || c == null || slotIndex < 0 || amount == 0 || conflict()) return false;
        boolean push = invertWheel.on() ? amount > 0 : amount < 0;
        if (shift && push) {
            Slot s = InventoryOps.slot(c, slotIndex);
            if (s == null || s.item().isEmpty()) return false;
            c.click(slotIndex, 0, Click.QUICK_MOVE);
            return true;
        }
        return push ? InventoryOps.pushOne(c, slotIndex) : InventoryOps.pullOne(c, slotIndex);
    }

    /** Left mouse button with Shift dragged over another slot. */
    public boolean onDrag(ContainerView c, int slotIndex, boolean shift) {
        if (!isActive() || !shiftDrag.on() || !shift || c == null || slotIndex < 0 || conflict()) return false;
        if (!c.cursor().isEmpty() || !dragged.add(slotIndex)) return false;
        Slot s = InventoryOps.slot(c, slotIndex);
        if (s == null || s.item().isEmpty()) return false;
        c.click(slotIndex, 0, Click.QUICK_MOVE);
        return true;
    }

    /** On Shift+click on the first slot remember it so that it is not counted twice when dragging. */
    public void onDragStart(int slotIndex) {
        dragged.clear();
        if (slotIndex >= 0) dragged.add(slotIndex);
    }

    public void onRelease() { dragged.clear(); }

    /** Middle click over a slot: sort its area. */
    public boolean onMiddleClick(ContainerView c, int slotIndex, boolean creative) {
        if (!isActive() || !middleSort.on() || c == null || slotIndex < 0 || conflict()) return false;
        if (creative && !c.isPlayerOnly()) return false; // middle click = copy item in creative mode
        Slot s = InventoryOps.slot(c, slotIndex);
        if (s == null) return false;
        return InventoryOps.sort(c, InventoryOps.sameRegion(c, s));
    }
}
