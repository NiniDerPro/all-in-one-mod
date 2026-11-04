package dev.aio.fabric;

import dev.aio.core.platform.ContainerView;
import dev.aio.core.platform.ItemRef;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;

import java.util.ArrayList;
import java.util.List;

/**
 * Offenes Inventar für Maus-Tweaks und Sortieren. Bei Menüs mit Sonderslots (Werkbank, Ofen, ...)
 * werden nur die Spielerslots einbezogen, damit nie aus Versehen ein Ergebnisslot geklickt wird.
 */
final class FabricContainerView implements ContainerView {
    private final Minecraft client;
    private final AbstractContainerMenu menu;
    private final boolean storage;

    FabricContainerView(Minecraft client, AbstractContainerMenu menu) {
        this.client = client;
        this.menu = menu;
        this.storage = menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu || menu instanceof HopperMenu
                || menu instanceof DispenserMenu;
    }

    @Override public List<ContainerView.Slot> slots() {
        List<ContainerView.Slot> out = new ArrayList<>(menu.slots.size());
        for (net.minecraft.world.inventory.Slot s : menu.slots) {
            boolean player = s.container instanceof Inventory;
            if (!player && !storage) continue;
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
}
