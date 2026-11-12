package dev.aio.core.modules.render;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;

/** Switches the camera swaying while walking off. */
public final class NoViewBobbingModule extends Module {
    public NoViewBobbingModule() {
        super("noviewbob", "No View Bobbing", "No camera swaying while walking", Category.VISUALS, false);
    }

    public boolean suppress() { return isActive(); }
}
