package dev.aio.core.modules.render;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;

/** No camera shake when taking damage. */
public final class NoHurtcamModule extends Module {
    public NoHurtcamModule() {
        super("nohurtcam", "No Hurtcam", "No camera shake when taking damage", Category.VISUALS, false);
    }

    public boolean suppress() { return isActive(); }
}
