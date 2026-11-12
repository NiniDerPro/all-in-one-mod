package dev.aio.core.modules.render;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.setting.NumberSetting;

/** Overrides the brightness (gamma) while active. The adapter reads {@link #gammaOverride()}. */
public final class FullbrightModule extends Module {
    private final NumberSetting gamma;

    public FullbrightModule() {
        super("fullbright", "Fullbright", "Maximum brightness, even in caves", Category.VISUALS, true);
        gamma = add(new NumberSetting("gamma", "Brightness", 15, 1, 15, 0.5, "x"));
    }

    /** @return gamma value or NaN when not active */
    public double gammaOverride() {
        return isActive() ? gamma.get() : Double.NaN;
    }
}
