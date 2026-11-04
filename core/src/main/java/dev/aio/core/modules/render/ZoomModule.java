package dev.aio.core.modules.render;

import dev.aio.core.event.EventBus;
import dev.aio.core.event.Events;
import dev.aio.core.module.Category;
import dev.aio.core.platform.Keys;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.NumberSetting;

/** Zoom mit einstellbarem Faktor, sanftem Übergang und Anpassen per Mausrad. */
public final class ZoomModule extends KeyActivatedModule {
    private final NumberSetting factor;
    private final BooleanSetting smooth;
    private final BooleanSetting scrollAdjust;
    private final BooleanSetting reduceSensitivity;

    private double sessionFactor = 1;
    private double current = 1;
    private long lastNanos = -1;

    public ZoomModule() {
        super("zoom", "Zoom", "Heranzoomen per Taste, Mausrad passt die Stärke an", Category.RENDER, Keys.C);
        factor = add(new NumberSetting("factor", "Zoomfaktor", 4, 1.5, 20, 0.5, "x"));
        smooth = add(new BooleanSetting("smooth", "Sanfter Übergang", true));
        scrollAdjust = add(new BooleanSetting("scroll", "Mausrad anpassen", true));
        reduceSensitivity = add(new BooleanSetting("sensitivity", "Maus verlangsamen", true));
        on(Events.MouseScroll.class, this::onScroll, EventBus.PRIORITY_HIGH);
    }

    @Override protected void onEngage() { sessionFactor = factor.get(); }

    @Override protected void onDisengage() {}

    @Override protected void onDeactivate() {
        super.onDeactivate();
        current = 1;
        lastNanos = -1;
    }

    private void onScroll(Events.MouseScroll e) {
        if (!isEngaged() || !scrollAdjust.on() || e.amount == 0) return;
        sessionFactor *= e.amount > 0 ? 1.15 : 1 / 1.15;
        sessionFactor = Math.max(1.1, Math.min(50, sessionFactor));
        e.cancel();
    }

    /** Pro Frame vom Adapter abgefragt: Faktor, mit dem das Sichtfeld multipliziert wird (1 = kein Zoom). */
    public double fovMultiplier() {
        if (!isActive()) return 1;
        double target = isEngaged() ? sessionFactor : 1;
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

    /** Aktueller FOV-Faktor ohne die Animation weiterzuschalten (1 = kein Zoom). */
    public double currentMultiplier() { return isActive() ? 1 / current : 1; }

    /** Faktor für die Mausempfindlichkeit, damit Zielen beim Zoomen nicht zu schnell ist. */
    public double sensitivityMultiplier() {
        if (!isActive() || !reduceSensitivity.on() || current <= 1) return 1;
        return 1 / current;
    }
}
