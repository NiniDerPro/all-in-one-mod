package dev.aio.core.modules.render;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.ColorSetting;
import dev.aio.core.setting.NumberSetting;

/**
 * Light Level Overlay: färbt die Oberseite von Blöcken, auf denen Monster stehen könnten.
 * Rot = Blocklicht 0, hier können Monster spawnen. Grün = durch Fackeln o. Ä. beleuchtet, sicher.
 * Sonnenlicht wird ignoriert (nachts/in Höhlen zählt nur Blocklicht). Es werden nur echte
 * Spawn-Oberflächen gezeigt (fester Boden, darüber zwei Blöcke frei), das hält es übersichtlich
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

    /** Einmalig: der frühere Standard 16 wird zum neuen Standard 64. */
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
