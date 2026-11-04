package dev.aio.core.modules.render;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;

/** Kein Kamerawackeln, wenn man Schaden bekommt. */
public final class NoHurtcamModule extends Module {
    public NoHurtcamModule() {
        super("nohurtcam", "No Hurtcam", "No camera shake when taking damage", Category.RENDER, false);
    }

    public boolean suppress() { return isActive(); }
}
