package dev.aio.core.modules.render;

import dev.aio.core.event.Events;
import dev.aio.core.module.Category;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.NumberSetting;

/**
 * Freecam: Die Kamera löst sich vom Spieler und fliegt frei durch Blöcke (WASD, Springen hoch,
 * Schleichen runter, Sprinten schneller). Der Spieler bleibt stehen und ist sichtbar; Abbauen und
 * Benutzen sind gesperrt. Taste F4 im Modus "Toggle": einmal drücken an, nochmal aus.
 */
public final class FreecamModule extends KeyActivatedModule {
    private final NumberSetting horizontalSpeed;
    private final NumberSetting verticalSpeed;
    private final BooleanSetting disableOnDamage;
    private final BooleanSetting notify;
    private final BooleanSetting noClip, showPlayer, showHand, allowInteract;

    public FreecamModule() {
        super("freecam", "Freecam", "Detach the camera and fly through blocks while your player stays put", Category.VISUALS,
                "key.keyboard.f4", ActivationMode.TOGGLE);
        horizontalSpeed = add(new NumberSetting("hSpeed", "Horizontal speed", 1.0, 0.1, 5.0, 0.1, "x"));
        verticalSpeed = add(new NumberSetting("vSpeed", "Vertical speed", 1.0, 0.1, 5.0, 0.1, "x"));
        disableOnDamage = add(new BooleanSetting("disableOnDamage", "Turn off when taking damage", true));
        notify = add(new BooleanSetting("notify", "Show on/off message", true));
        noClip = add(new BooleanSetting("noClip", "Fly through blocks", true));
        showPlayer = add(new BooleanSetting("showPlayer", "Show your player", true));
        showHand = add(new BooleanSetting("showHand", "Show hand", false));
        allowInteract = add(new BooleanSetting("allowInteract", "Allow interacting (break/use)", false));
        // Von außen beendet (Schaden, Tod, Weltwechsel): Zustand angleichen, damit F4 wieder einschaltet
        on(Events.Tick.class, e -> { if (isEngaged() && !platform().freecamActive()) setEngaged(false); });
    }

    @Override protected void onEngage() {
        platform().setFreecam(true);
        if (notify.on()) platform().notify(platform().freecamActive() ? "Freecam on" : "Freecam could not start");
    }

    @Override protected void onDisengage() {
        boolean was = platform().freecamActive();
        platform().setFreecam(false);
        if (was && notify.on()) platform().notify("Freecam off");
    }

    public double horizontalSpeed() { return horizontalSpeed.get(); }

    public double verticalSpeed() { return verticalSpeed.get(); }

    public boolean disableOnDamage() { return disableOnDamage.on(); }

    public boolean noClip() { return noClip.on(); }

    public boolean showPlayer() { return showPlayer.on(); }

    public boolean showHand() { return showHand.on(); }

    public boolean allowInteract() { return allowInteract.on(); }
}
