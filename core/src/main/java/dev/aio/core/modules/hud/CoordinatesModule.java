package dev.aio.core.modules.hud;

import dev.aio.core.hud.TextHudElement;
import dev.aio.core.platform.Vec3;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.NumberSetting;

import java.util.Locale;

/** Koordinaten, optional mit Blickrichtung und Biom. */
public final class CoordinatesModule extends TextHudElement {
    private static final String[] DIRECTIONS = {"S", "SW", "W", "NW", "N", "NE", "O", "SE"};

    private final NumberSetting decimals;
    private final BooleanSetting showDirection;
    private final BooleanSetting showBiome;

    public CoordinatesModule() {
        super("coordinates", "Coordinates", "Position, facing and biome", dev.aio.core.module.Category.QOL, false, 0.0, 0.12);
        decimals = add(new NumberSetting("decimals", "Decimals", 0, 0, 2, 1));
        showDirection = add(new BooleanSetting("showDirection", "Facing", true));
        showBiome = add(new BooleanSetting("showBiome", "Biome", false));
    }

    @Override protected String getText() {
        if (!platform().inWorld()) return "";
        Vec3 p = platform().playerPos();
        String fmt = "%." + decimals.intValue() + "f";
        StringBuilder sb = new StringBuilder(String.format(Locale.ROOT, "XYZ: " + fmt + " / " + fmt + " / " + fmt, p.x(), p.y(), p.z()));
        if (showDirection.on()) sb.append("  ").append(direction(platform().playerYaw()));
        if (showBiome.on()) sb.append("  ").append(platform().biome());
        return sb.toString();
    }

    /** Yaw 0 = Süden (MC-Konvention). */
    static String direction(float yaw) {
        int idx = Math.floorMod(Math.round(yaw / 45f), 8);
        return DIRECTIONS[idx];
    }

    @Override protected String previewText() { return "XYZ: 128 / 64 / -256  N"; }
}
