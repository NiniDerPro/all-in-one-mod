package dev.aio.core.modules.render;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.setting.NumberSetting;

/** Less (or no) camera shake when taking damage. */
public final class NoHurtcamModule extends Module {
    private final NumberSetting strength;

    public NoHurtcamModule() {
        super("nohurtcam", "No Hurtcam", "Reduce or remove the camera shake when taking damage", Category.VISUALS, false);
        strength = add(new NumberSetting("strength", "Shake that remains", 0, 0, 100, 5, "%"));
    }

    /** True if the shake is removed completely. */
    public boolean suppress() { return isActive() && strength.get() <= 0; }

    /** Part of the shake that stays (0 = none, 1 = vanilla). */
    public double remaining() { return isActive() ? Math.max(0, Math.min(1, strength.get() / 100.0)) : 1; }
}
