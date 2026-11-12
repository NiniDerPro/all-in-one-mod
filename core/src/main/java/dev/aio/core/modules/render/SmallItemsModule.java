package dev.aio.core.modules.render;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.NumberSetting;

/**
 * Smaller items in hand (first person): shield and totem otherwise cover a lot of the picture in PvP or when flying
 * with the trident (riptide). Purely visual, changes nothing in the game.
 */
public final class SmallItemsModule extends Module {
    private final BooleanSetting shield, totem, trident;
    private final NumberSetting size, lower;

    public SmallItemsModule() {
        super("smallitems", "Small Shield & Totem", "Shrinks shield and totem in your hand so they don't block the view", Category.VISUALS, false);
        shield = add(new BooleanSetting("shield", "Shield", true));
        totem = add(new BooleanSetting("totem", "Totem", true));
        trident = add(new BooleanSetting("trident", "Trident", false));
        size = add(new NumberSetting("size", "Size", 60, 20, 100, 5, " %"));
        lower = add(new NumberSetting("lower", "Lower", 10, 0, 50, 1, ""));
    }

    /** Which items are affected, by item id (e.g. "minecraft:shield"). */
    public boolean affects(String itemId) {
        if (!isActive()) return false;
        return switch (itemId) {
            case "minecraft:shield" -> shield.on();
            case "minecraft:totem_of_undying" -> totem.on();
            case "minecraft:trident" -> trident.on();
            default -> false;
        };
    }

    /** Skalierung 0.2..1. */
    public float scale() { return (float) (size.get() / 100.0); }

    /** How far down (in blocks of the hand view). */
    public float lowerBy() { return (float) (lower.get() / 100.0); }
}
