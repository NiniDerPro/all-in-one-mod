package dev.aio.core.modules.performance;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.setting.NumberSetting;

/** Dynamic FPS: in the background (Alt+Tab) the game runs with a few frames per second. */
public final class DynamicFpsModule extends Module {
    private final NumberSetting unfocused;

    public DynamicFpsModule() {
        super("dynamicfps", "Dynamic FPS", "Lower FPS while in the background: saves power, fans and GPU", Category.PERFORMANCE, false);
        unfocused = add(new NumberSetting("unfocused", "In background", 20, 1, 120, 1, " FPS"));
    }


    /** FPS limit for the window state or -1 = no change. */
    public int limit(boolean focused) {
        if (!isActive() || focused) return -1;
        return unfocused.intValue();
    }
}
