package dev.aio.core.modules.player;

import dev.aio.core.i18n.Lang;

import dev.aio.core.event.Events;
import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.platform.ContainerView;
import dev.aio.core.platform.ItemRef;
import dev.aio.core.setting.KeybindSetting;

import java.util.List;

/**
 * Swaps elytra and chestplate with a key: whoever wears the chestplate gets the elytra from the
 * inventory put on and vice versa. With several chestplates the strongest wins.
 */
public final class ElytraSwapModule extends Module {
    /** Inventory index of the chest slot (vanilla: 36 feet, 37 legs, 38 chest, 39 head). */
    public static final int CHEST_INVENTORY_SLOT = 38;
    private static final String ELYTRA = "minecraft:elytra";
    /** Order from weak to strong, unknown chestplates count like iron. */
    private static final String[] TIERS = {"leather", "copper", "golden", "chainmail", "iron", "diamond", "netherite"};

    private final KeybindSetting key;

    public ElytraSwapModule() {
        super("elytraswap", "Elytra Swapper", "Swap between elytra and chestplate with a key", Category.PVP, false);
        key = add(new KeybindSetting("key", "Key", "key.keyboard.grave.accent"));
        on(Events.Key.class, e -> {
            // also in the open player inventory (swap() checks there that only the own inventory is open)
            if (e.isPress() && key.triggeredBy(platform(), e.key()) && (!e.screenOpen() || platform().containerScreenOpen())) swap();
        });
    }


    /** Performs the swap. @return message for the player or null on success */
    public String swap() {
        ContainerView c = platform().container();
        if (c == null || !c.isPlayerOnly() || !c.cursor().isEmpty()) return null;
        int[] plan = plan(c.slots());
        if (plan == null) {
            platform().notify(Lang.tr("aio.ui.elytra_swap.no_elytra_or_chestplate_in", "No elytra or chestplate in your inventory"));
            return "nothing to swap";
        }
        // Pick up, put on the chest slot (swaps), put the old item back
        c.click(plan[0], 0, ContainerView.Click.PICKUP);
        c.click(plan[1], 0, ContainerView.Click.PICKUP);
        c.click(plan[0], 0, ContainerView.Click.PICKUP);
        return null;
    }

    /**
     * Which slot should go to the chest slot: {source, chest slot} or null.
     * Elytra getragen → beste Brustplatte; Brustplatte oder nichts getragen → Elytra.
     */
    public static int[] plan(List<ContainerView.Slot> slots) {
        ContainerView.Slot chest = null;
        for (ContainerView.Slot s : slots) {
            if (s.playerInventory() && s.containerSlot() == CHEST_INVENTORY_SLOT) chest = s;
        }
        if (chest == null) return null;
        ItemRef worn = chest.item();
        boolean wearingElytra = !worn.isEmpty() && ELYTRA.equals(worn.id());
        if (!worn.isEmpty() && !wearingElytra && !isChestplate(worn)) return null; // fremdes Item: nicht anfassen

        ContainerView.Slot best = null;
        int bestScore = -1;
        // Main inventory before the hotbar so that nothing disappears from the hand if it can be done differently
        for (int pass = 0; pass < 3 && best == null; pass++) {
            for (ContainerView.Slot s : slots) {
                if (!s.playerInventory() || s.item().isEmpty() || s == chest) continue;
                int cs = s.containerSlot();
                boolean inPass = pass == 0 ? cs >= 9 && cs < 36 : pass == 1 ? cs == 40 : cs >= 0 && cs < 9;
                if (!inPass) continue;
                if (wearingElytra) {
                    if (!isChestplate(s.item())) continue;
                    int score = tier(s.item().id()) * 10000 + (s.item().maxDamage() - s.item().damage());
                    if (score > bestScore) { best = s; bestScore = score; }
                } else if (ELYTRA.equals(s.item().id())) {
                    int score = s.item().maxDamage() - s.item().damage(); // haltbarste Elytra
                    if (score > bestScore) { best = s; bestScore = score; }
                }
            }
        }
        return best == null ? null : new int[]{best.index(), chest.index()};
    }

    static boolean isChestplate(ItemRef item) { return item.id().endsWith("_chestplate"); }

    static int tier(String id) {
        for (int i = TIERS.length - 1; i >= 0; i--) if (id.contains(TIERS[i])) return i + 1;
        return 5;
    }
}
