package dev.aio.core.modules.player;

import dev.aio.core.event.Events;
import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.platform.ContainerView;
import dev.aio.core.platform.ItemRef;
import dev.aio.core.setting.KeybindSetting;

import java.util.List;

/**
 * Tauscht per Taste Elytra und Brustplatte: Wer die Brustplatte trägt, bekommt die Elytra aus dem
 * Inventar angezogen und umgekehrt. Bei mehreren Brustplatten gewinnt die stärkste.
 */
public final class ElytraSwapModule extends Module {
    /** Inventar-Index des Brust-Slots (Vanilla: 36 Füße, 37 Beine, 38 Brust, 39 Kopf). */
    public static final int CHEST_INVENTORY_SLOT = 38;
    private static final String ELYTRA = "minecraft:elytra";
    /** Reihenfolge von schwach nach stark, unbekannte Brustplatten zählen wie Eisen. */
    private static final String[] TIERS = {"leather", "copper", "golden", "chainmail", "iron", "diamond", "netherite"};

    private final KeybindSetting key;

    public ElytraSwapModule() {
        super("elytraswap", "Elytra Swapper", "Swap between elytra and chestplate with a key", Category.PVP, false);
        key = add(new KeybindSetting("key", "Key", "key.keyboard.grave.accent"));
        on(Events.Key.class, e -> {
            // auch im offenen Spielerinventar (dort prüft swap(), dass nur das eigene Inventar offen ist)
            if (e.isPress() && key.triggeredBy(platform(), e.key()) && (!e.screenOpen() || platform().containerScreenOpen())) swap();
        });
    }


    /** Führt den Tausch aus. @return Meldung für den Spieler oder null bei Erfolg */
    public String swap() {
        ContainerView c = platform().container();
        if (c == null || !c.isPlayerOnly() || !c.cursor().isEmpty()) return null;
        int[] plan = plan(c.slots());
        if (plan == null) {
            platform().notify("No elytra or chestplate in your inventory");
            return "nothing to swap";
        }
        // Aufnehmen, auf den Brust-Slot legen (tauscht), alten Gegenstand zurücklegen
        c.click(plan[0], 0, ContainerView.Click.PICKUP);
        c.click(plan[1], 0, ContainerView.Click.PICKUP);
        c.click(plan[0], 0, ContainerView.Click.PICKUP);
        return null;
    }

    /**
     * Welcher Slot auf den Brust-Slot soll: {Quelle, Brust-Slot} oder null.
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
        // Hauptinventar vor der Hotbar, damit nichts aus der Hand verschwindet, wenn es anders geht
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
