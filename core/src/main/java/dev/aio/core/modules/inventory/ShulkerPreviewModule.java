package dev.aio.core.modules.inventory;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.platform.Keys;
import dev.aio.core.setting.BooleanSetting;

/** Zeigt den Inhalt von Shulkerkisten im Tooltip als Raster (mit Shift oder immer). */
public final class ShulkerPreviewModule extends Module {
    private final BooleanSetting requireShift;
    private final BooleanSetting tint;

    public ShulkerPreviewModule() {
        super("shulkerpreview", "Shulker-Vorschau", "Inhalt von Shulkerkisten im Tooltip (Shift halten)", Category.INVENTORY, false);
        requireShift = add(new BooleanSetting("requireShift", "Nur mit Shift", true));
        tint = add(new BooleanSetting("tint", "In Kistenfarbe", true));
    }

    @Override public boolean enabledByDefault() { return true; }

    public boolean shouldPreview() {
        if (!isActive()) return false;
        return !requireShift.on() || platform().isKeyDown(Keys.LEFT_SHIFT) || platform().isKeyDown(Keys.RIGHT_SHIFT);
    }

    /** Hinweis im normalen Tooltip, dass Shift den Inhalt zeigt. */
    public boolean showHint() { return isActive() && requireShift.on() && !shouldPreview(); }

    public boolean tint() { return tint.on(); }
}
