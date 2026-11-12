package dev.aio.core.modules.render;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.ColorSetting;
import dev.aio.core.setting.NumberSetting;

/**
 * Light Level Overlay: colours the top of blocks on which monsters could stand.
 * Red = block light 0, monsters can spawn here. Green = lit by torches or similar, safe.
 * Sunlight is ignored (at night/in caves only block light counts). Only real
 * spawn surfaces are shown (solid ground, two blocks free above), which keeps it tidy
 * und schnell.
 */
public final class LightOverlayModule extends Module {
    private final NumberSetting radius;
    private final BooleanSetting showSafe;
    private final ColorSetting danger, safe;
    private final NumberSetting opacity;
    private final BooleanSetting radiusMigrated;

    public LightOverlayModule() {
        super("lightoverlay", "Light Level Overlay", "Red where mobs can spawn, green where it's lit (block light only)", Category.VISUALS, false);
        radius = add(new NumberSetting("radius", "Radius", 64, 8, 512, 8, " blocks"));
        showSafe = add(new BooleanSetting("showSafe", "Show lit blocks (green)", true));
        danger = add(new ColorSetting("dangerColor", "Spawnable", 0xFFFF3030));
        safe = add(new ColorSetting("safeColor", "Lit", 0xFF30FF30));
        safe.visibleWhen(showSafe::on);
        opacity = add(new NumberSetting("opacity", "Opacity", 35, 10, 90, 5, " %"));
        radiusMigrated = add(new BooleanSetting("radiusMigrated", "Radius migrated", false));
        radiusMigrated.hidden();
    }

    public int radius() { return radius.intValue(); }

    /** Once: the former default 16 becomes the new default 64. */
    @Override public void afterLoad() {
        if (!radiusMigrated.on()) {
            if (radius.intValue() == 16) radius.reset();
            radiusMigrated.set(true);
        }
    }

    public boolean showSafe() { return showSafe.on(); }

    private int withOpacity(int argb) { return ((int) Math.round(opacity.get() * 2.55) << 24) | (argb & 0xFFFFFF); }

    public int dangerColor() { return withOpacity(danger.argb()); }

    public int safeColor() { return withOpacity(safe.argb()); }
}
