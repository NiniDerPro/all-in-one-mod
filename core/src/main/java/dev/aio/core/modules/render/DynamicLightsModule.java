package dev.aio.core.modules.render;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.EnumSetting;
import dev.aio.core.setting.NumberSetting;

/**
 * Dynamic Lights: glowing items (torch, lantern, glowstone, lava bucket, …) in the hand, lying on the
 * ground and burning entities light up the surroundings without a
 * block is placed. Purely visual.
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
        mode = add(new EnumSetting<>("mode", "Light sources", Mode.SELF));
        updateEvery = add(new NumberSetting("updateEvery", "Update every", 2, 1, 10, 1, " ticks"));
        entitiesLit = add(new BooleanSetting("entities", "Also light up mobs and players", true));
        maxSources = add(new NumberSetting("maxSources", "Max light sources", 12, 1, 64, 1, ""));
        maxSources.visibleWhen(() -> mode.get() != Mode.SELF);
        adaptive = add(new BooleanSetting("adaptive", "Update less often at low FPS", true));
    }

    private final NumberSetting maxSources;
    private final BooleanSetting adaptive;

    /** Most light sources that are updated at once (the nearest ones). */
    public int maxSources() { return mode.get() == Mode.SELF ? 1 : maxSources.intValue(); }

    /**
     * Ticks between updates; with "adaptive" doubled below 40 FPS and tripled below 25 FPS, because
     * every moving light makes Minecraft rebuild chunk sections.
     */
    public int updateEvery(int fps) {
        int base = updateEvery.intValue();
        if (!adaptive.on() || fps <= 0) return base;
        return fps < 25 ? base * 3 : fps < 40 ? base * 2 : base;
    }

    public Mode mode() { return mode.get(); }

    public int updateEvery() { return updateEvery.intValue(); }

    public boolean entitiesLit() { return entitiesLit.on(); }
}
