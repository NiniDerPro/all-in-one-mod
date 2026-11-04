package dev.aio.core.modules.render;

import dev.aio.core.event.Events;
import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.KeybindSetting;
import dev.aio.core.setting.NumberSetting;

/**
 * Freecam: Die Kamera löst sich vom Spieler und fliegt frei durch Blöcke (WASD, Springen hoch,
 * Schleichen runter, Sprinten schneller). Der Spieler bleibt stehen und ist sichtbar; Abbauen und
 * Benutzen sind gesperrt. Gleiche Taste oder Schaden beendet Freecam und die Kamera springt zurück.
 */
public final class FreecamModule extends Module {
    private final KeybindSetting key;
    private final NumberSetting horizontalSpeed;
    private final NumberSetting verticalSpeed;
    private final BooleanSetting disableOnDamage;
    private final BooleanSetting notify;

    public FreecamModule() {
        super("freecam", "Freecam", "Detach the camera and fly through blocks while your player stays put", Category.RENDER, true);
        key = add(new KeybindSetting("key", "Key", "key.keyboard.f4"));
        horizontalSpeed = add(new NumberSetting("hSpeed", "Horizontal speed", 1.0, 0.1, 5.0, 0.1, "x"));
        verticalSpeed = add(new NumberSetting("vSpeed", "Vertical speed", 1.0, 0.1, 5.0, 0.1, "x"));
        disableOnDamage = add(new BooleanSetting("disableOnDamage", "Turn off when taking damage", true));
        notify = add(new BooleanSetting("notify", "Show on/off message", true));
        on(Events.Key.class, e -> {
            if (e.isPress() && !e.screenOpen() && key.matches(e.key())) toggle();
        });
        on(Events.ServerLeave.class, e -> platform().setFreecam(false));
    }

    public void toggle() {
        boolean now = !platform().freecamActive();
        if (now && !platform().inWorld()) return;
        platform().setFreecam(now);
        if (notify.on()) platform().notify(now ? "Freecam enabled" : "Freecam disabled");
    }

    @Override protected void onDeactivate() { platform().setFreecam(false); }

    public double horizontalSpeed() { return horizontalSpeed.get(); }

    public double verticalSpeed() { return verticalSpeed.get(); }

    public boolean disableOnDamage() { return disableOnDamage.on(); }

}
