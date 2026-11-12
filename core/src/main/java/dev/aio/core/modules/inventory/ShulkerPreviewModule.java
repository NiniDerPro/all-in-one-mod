package dev.aio.core.modules.inventory;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.platform.Keys;
import dev.aio.core.setting.BooleanSetting;

/** Shows the contents of shulker boxes in the tooltip as a grid (with Shift or always). */
public final class ShulkerPreviewModule extends Module {
    private final BooleanSetting requireShift;
    private final BooleanSetting tint;

    public ShulkerPreviewModule() {
        super("shulkerpreview", "Shulker Preview", "Shulker box contents in the tooltip (hold Shift)", Category.QOL, false);
        requireShift = add(new BooleanSetting("requireShift", "Only with Shift", true));
        tint = add(new BooleanSetting("tint", "Box color", true));
    }


    public boolean shouldPreview() {
        if (!isActive()) return false;
        return !requireShift.on() || platform().isKeyDown(Keys.LEFT_SHIFT) || platform().isKeyDown(Keys.RIGHT_SHIFT);
    }

    /** Note in the normal tooltip that Shift shows the contents. */
    public boolean showHint() { return isActive() && requireShift.on() && !shouldPreview(); }

    public boolean tint() { return tint.on(); }
}
