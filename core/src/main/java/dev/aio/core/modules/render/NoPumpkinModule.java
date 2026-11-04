package dev.aio.core.modules.render;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;

/** Blendet das Kürbis-Overlay aus, wenn man einen geschnitzten Kürbis trägt. */
public final class NoPumpkinModule extends Module {
    public NoPumpkinModule() {
        super("nopumpkin", "No Pumpkin Overlay", "No overlay when wearing a pumpkin", Category.RENDER, true);
    }

    public boolean suppress() { return isActive(); }
}
