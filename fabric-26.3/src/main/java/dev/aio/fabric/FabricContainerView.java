package dev.aio.fabric;

import dev.aio.core.platform.ContainerView;
import dev.aio.core.platform.ItemRef;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import dev.aio.core.platform.RecipeKind;
import net.minecraft.world.inventory.BlastFurnaceMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.SmokerMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Offenes Inventar für Maus-Tweaks und Sortieren. Bei Menüs mit Sonderslots (Werkbank, Ofen, ...)
 * werden nur die Spielerslots einbezogen, damit nie aus Versehen ein Ergebnisslot geklickt wird.
 */
final class FabricContainerView implements ContainerView {
    private final Minecraft client;
    private final AbstractContainerMenu menu;
    private final boolean storage;
    private final Grid grid;

    FabricContainerView(Minecraft client, AbstractContainerMenu menu) {
        this.client = client;
        this.menu = menu;
        this.storage = menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu || menu instanceof HopperMenu
                || menu instanceof DispenserMenu;
        this.grid = gridOf(menu);
    }

    /** Eingabeslots nach den festen Slot-Reihenfolgen der Vanilla-Menüs. */
    private static Grid gridOf(AbstractContainerMenu menu) {
        if (menu instanceof InventoryMenu) return new Grid(Set.of(RecipeKind.CRAFTING), 2, 2, new int[]{1, 2, 3, 4});
        if (menu instanceof CraftingMenu) return new Grid(Set.of(RecipeKind.CRAFTING), 3, 3, new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9});
        if (menu instanceof BlastFurnaceMenu) return new Grid(Set.of(RecipeKind.BLAST_FURNACE), 1, 1, new int[]{0});
        if (menu instanceof SmokerMenu) return new Grid(Set.of(RecipeKind.SMOKER), 1, 1, new int[]{0});
        if (menu instanceof FurnaceMenu) return new Grid(Set.of(RecipeKind.FURNACE), 1, 1, new int[]{0});
        if (menu instanceof StonecutterMenu) return new Grid(Set.of(RecipeKind.STONECUTTER), 1, 1, new int[]{0});
        return null;
    }

    private boolean isGridSlot(int index) {
        if (grid == null) return false;
        for (int i : grid.slots()) if (i == index) return true;
        return false;
    }

    @Override public Grid grid() { return grid; }

    @Override public List<ContainerView.Slot> slots() {
        List<ContainerView.Slot> out = new ArrayList<>(menu.slots.size());
        for (net.minecraft.world.inventory.Slot s : menu.slots) {
            boolean player = s.container instanceof Inventory;
            // Ergebnisslots nie, Rasterslots immer (für Rezept-Übertragung), sonst nur echte Truhen
            if (!player && !storage && !isGridSlot(s.index)) continue;
            out.add(new ContainerView.Slot(s.index, FabricItemRef.of(s.getItem()), player, s.getContainerSlot()));
        }
        return out;
    }

    @Override public ItemRef cursor() { return FabricItemRef.of(menu.getCarried()); }

    @Override public void click(int slotIndex, int button, Click type) {
        if (client.gameMode == null || client.player == null) return;
        ContainerInput input = type == Click.QUICK_MOVE ? ContainerInput.QUICK_MOVE : ContainerInput.PICKUP;
        client.gameMode.handleContainerInput(menu.containerId, slotIndex, button, input, client.player);
    }

    @Override public boolean isPlayerOnly() { return menu instanceof InventoryMenu; }

    /** Vanilla Quick Craft (Ziehen mit links): Start, ein Paket pro Slot, Ende. */
    @Override public void dragSplit(int[] slots) {
        if (client.gameMode == null || client.player == null || slots.length == 0) return;
        int id = menu.containerId;
        client.gameMode.handleContainerInput(id, -999, quickcraftMask(0, 0), ContainerInput.QUICK_CRAFT, client.player);
        for (int s : slots) client.gameMode.handleContainerInput(id, s, quickcraftMask(1, 0), ContainerInput.QUICK_CRAFT, client.player);
        client.gameMode.handleContainerInput(id, -999, quickcraftMask(2, 0), ContainerInput.QUICK_CRAFT, client.player);
    }

    /** Wie {@code AbstractContainerMenu.getQuickcraftMask}: Phase (0 Start, 1 Slot, 2 Ende) + Art (0 = gleichmäßig). */
    private static int quickcraftMask(int phase, int type) { return (phase & 3) | ((type & 3) << 2); }
}
