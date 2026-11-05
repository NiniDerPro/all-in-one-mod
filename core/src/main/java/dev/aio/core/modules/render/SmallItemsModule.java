package dev.aio.core.modules.render;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.NumberSetting;

/**
 * Kleinere Items in der Hand (Ego-Sicht): Schild und Totem verdecken beim PvP oder beim Fliegen
 * mit dem Dreizack (Sog) sonst viel vom Bild. Rein optisch, ändert nichts am Spiel.
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

    /** Welche Items betroffen sind, nach Item-ID (z. B. "minecraft:shield"). */
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

    /** Wie weit nach unten (in Blöcken der Hand-Ansicht). */
    public float lowerBy() { return (float) (lower.get() / 100.0); }
}
