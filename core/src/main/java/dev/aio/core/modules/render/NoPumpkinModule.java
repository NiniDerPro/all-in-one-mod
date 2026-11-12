package dev.aio.core.modules.render;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;

/** Hides the pumpkin overlay when wearing a carved pumpkin. */
public final class NoPumpkinModule extends Module {
    public NoPumpkinModule() {
        super("nopumpkin", "No Pumpkin Overlay", "No overlay when wearing a pumpkin", Category.VISUALS, true);
    }

    public boolean suppress() { return isActive(); }
}
