package dev.aio.core.modules.render;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.EnumSetting;
import dev.aio.core.setting.NumberSetting;

/**
 * Dynamic Lights: glowing items (torch, lantern, glowstone, lava bucket, …) in the hand, lying on the
 * ground and burning entities light up the surroundings without a
 * Block gesetzt wird. Rein optisch.
 */
public final class DynamicLightsModule extends Module {
    public enum Mode implements EnumSetting.Named {
        SELF("Only you (fastest)"), PLAYERS("Players"), ALL("Players, items and burning entities");

        private final String displayName;

        Mode(String displayName) { this.displayName = displayName; }

        @Override public String displayName() { return displayName; }
    }

    private final EnumSetting<Mode> mode;
    private final NumberSetting updateEvery;
    private final BooleanSetting entitiesLit;

    public DynamicLightsModule() {
        super("dynamiclights", "Dynamic Lights", "Torches and other light items in your hand light up the area", Category.VISUALS, false);
        mode = add(new EnumSetting<>("mode", "Light sources", Mode.ALL));
        updateEvery = add(new NumberSetting("updateEvery", "Update every", 2, 1, 10, 1, " ticks"));
        entitiesLit = add(new BooleanSetting("entities", "Also light up mobs and players", true));
    }

    public Mode mode() { return mode.get(); }

    public int updateEvery() { return updateEvery.intValue(); }

    public boolean entitiesLit() { return entitiesLit.on(); }
}
