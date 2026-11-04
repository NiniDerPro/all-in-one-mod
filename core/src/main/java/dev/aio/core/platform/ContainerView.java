package dev.aio.core.platform;

import java.util.List;

/**
 * Das gerade offene Inventar (Truhe, Spielerinventar, ...) für Maus-Tweaks und Sortieren.
 * Klicks werden wie echte Mausklicks an den Server geschickt; der Client sagt sie sofort voraus,
 * {@link #slots()} zeigt danach den neuen Stand.
 */
public interface ContainerView {
    record Slot(int index, ItemRef item, boolean playerInventory, int containerSlot) {
        /** Hauptinventar des Spielers ohne Hotbar, Rüstung und Zweithand. */
        public boolean playerMain() { return playerInventory && containerSlot >= 9 && containerSlot < 36; }

        public boolean hotbar() { return playerInventory && containerSlot >= 0 && containerSlot < 9; }
    }

    enum Click { PICKUP, QUICK_MOVE }

    List<Slot> slots();

    ItemRef cursor();

    /** button 0 = links, 1 = rechts. */
    void click(int slotIndex, int button, Click type);

    /** Ob nur das Spielerinventar offen ist (keine Truhe o. Ä.). */
    boolean isPlayerOnly();

    /**
     * Eingabe-Raster dieses Menüs (2×2 im Inventar, 3×3 Werkbank, Ofen-Eingang, ...).
     *
     * @param kinds  Rezeptarten, die hier hergestellt werden können
     * @param slots  Menü-Indizes der Eingabeslots, zeilenweise (width × height)
     */
    record Grid(java.util.Set<RecipeKind> kinds, int width, int height, int[] slots) {}

    /** Raster oder null, wenn in diesem Menü nichts hergestellt werden kann. */
    default Grid grid() { return null; }
}
