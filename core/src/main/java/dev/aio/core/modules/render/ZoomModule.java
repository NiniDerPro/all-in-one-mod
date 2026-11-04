package dev.aio.core.modules.render;

import dev.aio.core.event.EventBus;
import dev.aio.core.event.Events;
import dev.aio.core.module.Category;
import dev.aio.core.platform.Keys;
import dev.aio.core.platform.VanillaOption;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.NumberSetting;

/**
 * Zoom wie bei bekannten Zoom-Mods: Taste halten = sofort 3-fach, Mausrad zoomt weiter rein/raus
 * (je ×1,1, 1× bis 50×, ohne den Hotbar-Slot zu wechseln). Die Maus wird so verlangsamt, als würde
 * die Empfindlichkeit durch die Zoomstufe geteilt; die Hand rückt mit aus dem Bild. Jeder neue
 * Druck beginnt wieder bei der Standardstufe.
 */
public final class ZoomModule extends KeyActivatedModule {
    // Neue IDs, damit alte Werte (4×, sanft) nicht übernommen werden
    private final NumberSetting level;
    private final BooleanSetting smooth;
    private final BooleanSetting scrollAdjust;
    private final BooleanSetting reduceSensitivity;

    private double sessionLevel = 3;
    private double current = 1;
    private long lastNanos = -1;

    public ZoomModule() {
        super("zoom", "Zoom", "Taste halten zum Zoomen, Mausrad ändert die Stufe", Category.RENDER, Keys.C);
        level = add(new NumberSetting("level", "Zoomstufe", 3, 1.5, 50, 0.5, "x"));
        smooth = add(new BooleanSetting("smoothZoom", "Sanfter Übergang", false));
        scrollAdjust = add(new BooleanSetting("scroll", "Mausrad ändert Stufe", true));
        reduceSensitivity = add(new BooleanSetting("sensitivity", "Maus verlangsamen", true));
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
        e.cancel(); // beim Zoomen nie den Hotbar-Slot wechseln
        if (!scrollAdjust.on()) return;
        sessionLevel *= e.amount > 0 ? 1.1 : 0.9;
        sessionLevel = Math.max(1, Math.min(50, sessionLevel));
    }

    /** Pro Frame vom Adapter abgefragt: Faktor, mit dem das Sichtfeld multipliziert wird (1 = kein Zoom). */
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

    /** Aktueller FOV-Faktor ohne die Animation weiterzuschalten (1 = kein Zoom). */
    public double currentMultiplier() { return isActive() ? 1 / current : 1; }

    /**
     * Faktor für die Mausbewegung. Minecraft rechnet die Empfindlichkeit s als (0,6·s + 0,2)³ um;
     * geteilt durch die Zoomstufe fühlt sich das an wie beim Teilen der Empfindlichkeits-Option.
     */
    public double sensitivityMultiplier() {
        if (!isActive() || !reduceSensitivity.on() || current <= 1) return 1;
        double s = platform().option(VanillaOption.MOUSE_SENSITIVITY);
        if (!Double.isFinite(s)) s = 0.5;
        double base = Math.pow(0.6 * s + 0.2, 3), zoomed = Math.pow(0.6 * s / current + 0.2, 3);
        return base <= 0 ? 1 / current : zoomed / base;
    }
}
