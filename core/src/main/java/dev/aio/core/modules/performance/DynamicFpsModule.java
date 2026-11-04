package dev.aio.core.modules.performance;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.setting.NumberSetting;

/** Dynamische FPS: Im Hintergrund (Alt+Tab) läuft das Spiel mit wenigen Bildern pro Sekunde. */
public final class DynamicFpsModule extends Module {
    private final NumberSetting unfocused;

    public DynamicFpsModule() {
        super("dynamicfps", "Dynamische FPS", "Weniger FPS im Hintergrund: spart Strom, Lüfter und Grafikkarte", Category.PERFORMANCE, false);
        unfocused = add(new NumberSetting("unfocused", "Im Hintergrund", 20, 1, 120, 1, " FPS"));
    }

    @Override public boolean enabledByDefault() { return true; }

    /** FPS-Grenze für den Fensterzustand oder -1 = keine Änderung. */
    public int limit(boolean focused) {
        if (!isActive() || focused) return -1;
        return unfocused.intValue();
    }
}
