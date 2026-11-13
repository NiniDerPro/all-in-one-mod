package dev.aio.neoforge;

import dev.aio.core.AioCore;
import dev.aio.core.modules.render.FreecamModule;
import dev.aio.neoforge.freecam.FreeCamera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;

/**
 * Freecam modelled on the Freecam mod (MIT, see freecam/NOTICE.md): an own
 * camera player ({@link FreeCamera}) is registered in the world and made the camera. The real
 * player gets an empty input meanwhile (only sneaking is kept) and stays put.
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

    // ---- Settings from the AIO module ----

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
            AioNeoForgeClient.LOGGER.info("[aio] Freecam {}", enabled ? "on" : "off");
        } catch (RuntimeException e) {
            AioNeoForgeClient.LOGGER.error("[aio] Freecam could not be switched", e);
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

        if (MC.gameRenderer.getMainCamera().isDetached()) MC.options.setCameraType(CameraType.FIRST_PERSON);

        freeCamera = new FreeCamera(-420);
        freeCamera.copyPosition(MC.player); // perspective "Inside": exactly at the player's head
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

    /** Empty input for the player; sneaking stays so that you do not fall off edges. */
    private static ClientInput updateDummyInput() {
        ClientInput input = new ClientInput();
        input.keyPresses = new Input(false, false, false, false, false, playerInput.keyPresses.shift(), false);
        return dummyPlayerInput = input;
    }

    /** Before every client tick (as in the Freecam mod). */
    public static void preTick() {
        if (disableNextTick && enabled) set(false);
        disableNextTick = false;
        if (enabled && MC.player != null) MC.player.input = dummyPlayerInput;
    }

    /** After every client tick: clean up if world/player are gone. */
    public static void tick() {
        if (!enabled) return;
        if (MC.player == null || MC.level == null || freeCamera == null || freeCamera.level() != MC.level || !MC.player.isAlive()) {
            set(false);
            return;
        }
        if (MC.getCameraEntity() != freeCamera) MC.setCameraEntity(freeCamera);
    }

    public static void disableNextTick() { disableNextTick = true; }

    /** Mouse movement turns the camera instead of the player. */
    public static boolean turn(double dx, double dy) {
        if (!enabled || freeCamera == null) return false;
        freeCamera.turn(dx, dy);
        return true;
    }
}
