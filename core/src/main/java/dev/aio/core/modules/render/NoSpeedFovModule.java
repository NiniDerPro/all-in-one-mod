package dev.aio.core.modules.render;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.setting.NumberSetting;

/** The field of view no longer widens when sprinting, flying or under speed effects (adjustable). */
public final class NoSpeedFovModule extends Module {
    private final NumberSetting strength;

    public NoSpeedFovModule() {
        super("nospeedfov", "No Speed FOV", "No field-of-view change from sprinting, flying and speed effects", Category.VISUALS, false);
        strength = add(new NumberSetting("strength", "FOV change that remains", 0, 0, 100, 5, "%"));
    }

    /** Part of the speed FOV change that stays (0 = none, 1 = vanilla). */
    public double remaining() { return isActive() ? Math.max(0, Math.min(1, strength.get() / 100.0)) : 1; }
}
