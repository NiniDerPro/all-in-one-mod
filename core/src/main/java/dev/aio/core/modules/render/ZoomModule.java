package dev.aio.core.modules.render;

import dev.aio.core.event.EventBus;
import dev.aio.core.event.Events;
import dev.aio.core.module.Category;
import dev.aio.core.platform.Keys;
import dev.aio.core.platform.VanillaOption;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.NumberSetting;

/**
 * Zoom: Taste halten = sofort 3-fach, Mausrad zoomt weiter rein/raus
 * (each ×1.1, 1× to 50×, without changing the hotbar slot). The mouse is slowed down as if
 * the sensitivity were divided by the zoom level; the hand moves out of the picture. Every new
 * press starts again at the default level.
 */
public final class ZoomModule extends KeyActivatedModule {
    // New ids so that old values (4×, smooth) are not taken over
    private final NumberSetting level;
    private final BooleanSetting smooth;
    private final BooleanSetting scrollAdjust;
    private final BooleanSetting reduceSensitivity;

    private double sessionLevel = 3;
    private double current = 1;
    private long lastNanos = -1;

    public ZoomModule() {
        super("zoom", "Zoom", "Hold the key to zoom, scroll to change the level", Category.VISUALS, Keys.C);
        level = add(new NumberSetting("level", "Zoom level", 3, 1.5, 50, 0.5, "x"));
        smooth = add(new BooleanSetting("smoothZoom", "Smooth transition", false));
        scrollAdjust = add(new BooleanSetting("scroll", "Scroll changes level", true));
        reduceSensitivity = add(new BooleanSetting("sensitivity", "Slow down mouse", true));
        on(Events.MouseScroll.class, this::onScroll, EventBus.PRIORITY_HIGH);
    }

    @Override protected void onEngage() { sessionLevel = level.get(); }

    @Override protected void onDisengage() {}

    @Override protected void onDeactivate() {
        super.onDeactivate();
        current = 1;
        lastNanos = -1;
    }

    private void onScroll(Events.MouseScroll e) {
        if (!isEngaged() || e.amount == 0) return;
        e.cancel(); // never change the hotbar slot while zooming
        if (!scrollAdjust.on()) return;
        sessionLevel *= e.amount > 0 ? 1.1 : 0.9;
        sessionLevel = Math.max(1, Math.min(50, sessionLevel));
    }

    /** Queried by the adapter every frame: factor by which the field of view is multiplied (1 = no zoom). */
    public double fovMultiplier() {
        if (!isActive()) return 1;
        double target = isEngaged() ? sessionLevel : 1;
        long now = System.nanoTime();
        if (!smooth.on() || lastNanos < 0) {
            current = target;
        } else {
            double dt = Math.min(0.1, (now - lastNanos) / 1e9);
            current += (target - current) * (1 - Math.exp(-dt * 14));
            if (Math.abs(current - target) < 0.001) current = target;
        }
        lastNanos = now;
        return 1 / current;
    }

    /** Current FOV factor without advancing the animation (1 = no zoom). */
    public double currentMultiplier() { return isActive() ? 1 / current : 1; }

    /**
     * Factor for the mouse movement. Minecraft converts the sensitivity s as (0.6·s + 0.2)³;
     * divided by the zoom level this feels like dividing the sensitivity option.
     */
    public double sensitivityMultiplier() {
        if (!isActive() || !reduceSensitivity.on() || current <= 1) return 1;
        double s = platform().option(VanillaOption.MOUSE_SENSITIVITY);
        if (!Double.isFinite(s)) s = 0.5;
        double base = Math.pow(0.6 * s + 0.2, 3), zoomed = Math.pow(0.6 * s / current + 0.2, 3);
        return base <= 0 ? 1 / current : zoomed / base;
    }
}
