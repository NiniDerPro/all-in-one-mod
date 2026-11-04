package dev.aio.fabric;

import dev.aio.core.AioCore;
import dev.aio.core.modules.render.FreecamModule;
import dev.aio.fabric.freecam.FreeCamera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;

/**
 * Freecam nach dem Vorbild der Freecam-Mod (MIT, siehe freecam/NOTICE.md): Ein eigener
 * Kamera-Spieler ({@link FreeCamera}) wird in der Welt angemeldet und zur Kamera gemacht. Der echte
 * Spieler bekommt solange eine leere Eingabe (nur Schleichen bleibt erhalten) und bleibt stehen.
 */
public final class Freecam {
    private Freecam() {}

    private static final Minecraft MC = Minecraft.getInstance();

    private static boolean enabled;
    private static boolean disableNextTick;
    private static FreeCamera freeCamera;
    private static CameraType rememberedF5;
    private static ClientInput playerInput;
    private static ClientInput dummyPlayerInput;

    // ---- Einstellungen aus dem AIO-Modul ----

    private static FreecamModule module() {
        AioCore core = AioCore.get();
        return core == null ? null : core.module(FreecamModule.class);
    }

    public static double horizontalSpeed() { FreecamModule m = module(); return m == null ? 1 : m.horizontalSpeed(); }

    public static double verticalSpeed() { FreecamModule m = module(); return m == null ? 1 : m.verticalSpeed(); }

    public static boolean noClip() { FreecamModule m = module(); return m == null || m.noClip(); }

    public static boolean showPlayer() { FreecamModule m = module(); return m == null || m.showPlayer(); }

    public static boolean hideHand() { FreecamModule m = module(); return m == null || !m.showHand(); }

    public static boolean preventInteractions() { FreecamModule m = module(); return m == null || !m.allowInteract(); }

    public static boolean disableOnDamage() { FreecamModule m = module(); return m == null || m.disableOnDamage(); }

    // ---- Zustand ----

    public static boolean active() { return enabled; }

    public static FreeCamera camera() { return freeCamera; }

    public static void set(boolean on) {
        if (on == enabled) return;
        try {
            if (on) {
                if (MC.player == null || MC.level == null) return;
                onEnable();
                enabled = true;
            } else {
                onDisable();
                enabled = false;
            }
            AioFabricClient.LOGGER.info("[aio] Freecam {}", enabled ? "on" : "off");
        } catch (RuntimeException e) {
            AioFabricClient.LOGGER.error("[aio] Freecam konnte nicht umgeschaltet werden", e);
            enabled = false;
            freeCamera = null;
            if (MC.player != null) {
                if (playerInput != null) MC.player.input = playerInput;
                MC.setCameraEntity(MC.player);
            }
        }
    }

    private static void onEnable() {
        MC.smartCull = false;
        rememberedF5 = MC.options.getCameraType();

        playerInput = MC.player.input;
        MC.player.input = updateDummyInput();

        if (MC.gameRenderer.mainCamera().isDetached()) MC.options.setCameraType(CameraType.FIRST_PERSON);

        freeCamera = new FreeCamera(-420);
        freeCamera.copyPosition(MC.player); // Perspektive "Inside": genau am Kopf des Spielers
        freeCamera.spawn();
        MC.setCameraEntity(freeCamera);
    }

    private static void onDisable() {
        MC.smartCull = true;
        if (MC.player != null) MC.setCameraEntity(MC.player);
        if (freeCamera != null) freeCamera.despawn();
        freeCamera = null;
        if (MC.player != null && playerInput != null) MC.player.input = playerInput;
        playerInput = null;
        dummyPlayerInput = null;
        if (rememberedF5 != null) MC.options.setCameraType(rememberedF5);
        rememberedF5 = null;
    }

    /** Leere Eingabe für den Spieler; Schleichen bleibt, damit man nicht von Kanten fällt. */
    private static ClientInput updateDummyInput() {
        ClientInput input = new ClientInput();
        input.keyPresses = new Input(false, false, false, false, false, playerInput.keyPresses.shift(), false);
        return dummyPlayerInput = input;
    }

    /** Vor jedem Client-Tick (wie in der Freecam-Mod). */
    public static void preTick() {
        if (disableNextTick && enabled) set(false);
        disableNextTick = false;
        if (enabled && MC.player != null) MC.player.input = dummyPlayerInput;
    }

    /** Nach jedem Client-Tick: aufräumen, falls Welt/Spieler weg sind. */
    public static void tick() {
        if (!enabled) return;
        if (MC.player == null || MC.level == null || freeCamera == null || freeCamera.level() != MC.level || !MC.player.isAlive()) {
            set(false);
            return;
        }
        if (MC.getCameraEntity() != freeCamera) MC.setCameraEntity(freeCamera);
    }

    public static void disableNextTick() { disableNextTick = true; }

    /** Mausbewegung dreht die Kamera statt des Spielers. */
    public static boolean turn(double dx, double dy) {
        if (!enabled || freeCamera == null) return false;
        freeCamera.turn(dx, dy);
        return true;
    }
}
