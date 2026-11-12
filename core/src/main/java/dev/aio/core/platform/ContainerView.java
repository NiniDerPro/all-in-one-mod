package dev.aio.core.platform;

import java.util.List;
import java.util.Set;

/**
 * The currently open inventory (chest, player inventory, ...) for mouse tweaks and sorting.
 * Clicks are sent to the server like real mouse clicks; the client predicts them immediately,
 * {@link #slots()} then shows the new state.
 */
public interface ContainerView {
    record Slot(int index, ItemRef item, boolean playerInventory, int containerSlot) {
        /** Main inventory of the player without hotbar, armor and offhand. */
        public boolean playerMain() { return playerInventory && containerSlot >= 9 && containerSlot < 36; }

        public boolean hotbar() { return playerInventory && containerSlot >= 0 && containerSlot < 9; }
    }

    enum Click { PICKUP, QUICK_MOVE }

    List<Slot> slots();

    ItemRef cursor();

    /** button 0 = links, 1 = rechts. */
    void click(int slotIndex, int button, Click type);

    /**
     * Distributes the stack on the cursor evenly over the slots, like dragging with the left
     * mouse button held (vanilla "quick craft"). The rest stays on the cursor. Few packets instead of many single clicks.
     */
    void dragSplit(int[] slotIndices);

    /** Whether only the player inventory is open (no chest or similar). */
    boolean isPlayerOnly();

    /**
     * Input grid of this menu (2×2 in the inventory, 3×3 crafting table, furnace input, ...).
     *
     * @param kinds  recipe kinds that can be made here
     * @param slots  menu indices of the input slots, row by row (width × height)
     */
    record Grid(Set<RecipeKind> kinds, int width, int height, int[] slots) {}

    /** Grid or null if nothing can be made in this menu. */
    default Grid grid() { return null; }
}
