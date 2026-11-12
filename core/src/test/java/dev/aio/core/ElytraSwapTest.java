package dev.aio.core;

import dev.aio.core.modules.player.ElytraSwapModule;
import dev.aio.core.platform.ContainerView.Slot;
import dev.aio.core.platform.ItemRef;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ElytraSwapTest {
    private record Item(String id, int damage, int maxDamage) implements ItemRef {
        @Override public boolean isEmpty() { return id.isEmpty(); }
        @Override public int count() { return 1; }
        @Override public String name() { return id; }
    }

    private static final Item EMPTY = new Item("", 0, 0);

    /** Player inventory like InventoryMenu: 5-8 armor (39..36), 9-35 main, 36-44 hotbar, 45 offhand. */
    private static List<Slot> inventory(String chest, String... extra) {
        List<Slot> out = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            int cs = 39 - i;
            out.add(new Slot(5 + i, cs == 38 && chest != null ? new Item(chest, 0, 400) : EMPTY, true, cs));
        }
        for (int i = 9; i < 36; i++) out.add(new Slot(i, EMPTY, true, i));
        for (int i = 0; i < 9; i++) out.add(new Slot(36 + i, EMPTY, true, i));
        // extra: "slotIndex=id"
        for (String e : extra) {
            String[] p = e.split("=");
            int idx = Integer.parseInt(p[0]);
            for (int j = 0; j < out.size(); j++) {
                Slot s = out.get(j);
                if (s.index() == idx) out.set(j, new Slot(idx, new Item(p[1], 0, 400), true, s.containerSlot()));
            }
        }
        return out;
    }

    @Test void chestplateWornSwapsToElytra() {
        assertArrayEquals(new int[]{20, 6}, ElytraSwapModule.plan(inventory("minecraft:diamond_chestplate", "20=minecraft:elytra")));
    }

    @Test void elytraWornSwapsToStrongestChestplate() {
        int[] plan = ElytraSwapModule.plan(inventory("minecraft:elytra",
                "12=minecraft:iron_chestplate", "15=minecraft:netherite_chestplate", "30=minecraft:leather_chestplate"));
        assertArrayEquals(new int[]{15, 6}, plan);
    }

    @Test void nothingWornEquipsElytraAndPrefersMainInventoryOverHotbar() {
        assertArrayEquals(new int[]{11, 6}, ElytraSwapModule.plan(inventory(null, "36=minecraft:elytra", "11=minecraft:elytra")));
    }

    @Test void foreignChestItemIsLeftAlone() {
        assertNull(ElytraSwapModule.plan(inventory("minecraft:carved_pumpkin", "20=minecraft:elytra")));
        assertNull(ElytraSwapModule.plan(inventory("minecraft:iron_chestplate")));
    }
}
