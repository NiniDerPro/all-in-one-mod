package dev.aio.fabric;

import dev.aio.core.AioCore;
import dev.aio.core.modules.render.FreecamModule;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.phys.Vec3;

/**
 * Freecam: ein unsichtbarer Kamera-Körper (Marker, nicht in der Welt angemeldet) wird zur Kamera.
 * Er fliegt ohne Kollision mit den Bewegungstasten; der Spieler bekommt solange keine Bewegung.
 */
public final class Freecam {
    private Freecam() {}

    private static Marker camera;
    private static CameraType previousView;

    public static boolean active() { return camera != null; }

    public static void set(boolean on) {
        Minecraft mc = Minecraft.getInstance();
        if (on == active()) return;
        if (on) {
            if (mc.player == null || mc.level == null) return;
            Marker cam = new Marker(net.minecraft.world.entity.EntityTypes.MARKER, mc.level);
            Vec3 eye = mc.player.getEyePosition();
            cam.setPos(eye.x, eye.y, eye.z);
            cam.setYRot(mc.player.getYRot());
            cam.setXRot(mc.player.getXRot());
            cam.setOldPosAndRot();
            camera = cam;
            previousView = mc.options.getCameraType();
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.setCameraEntity(cam);
        } else {
            camera = null;
            if (mc.player != null) mc.setCameraEntity(mc.player);
            if (previousView != null) mc.options.setCameraType(previousView);
            previousView = null;
        }
    }

    /** Mausbewegung dreht die Kamera statt des Spielers. */
    public static boolean turn(double dx, double dy) {
        if (camera == null) return false;
        camera.turn(dx, dy);
        return true;
    }

    /** Jeden Client-Tick: Kamera bewegen, bei Schaden/Weltwechsel/Tod beenden. */
    public static void tick() {
        if (camera == null) return;
        Minecraft mc = Minecraft.getInstance();
        AioCore core = AioCore.get();
        FreecamModule m = core == null ? null : core.module(FreecamModule.class);
        if (mc.player == null || mc.level == null || camera.level() != mc.level || !mc.player.isAlive()
                || m == null || !m.isActive() || m.disableOnDamage() && mc.player.hurtTime > 0) {
            set(false);
            return;
        }
        if (mc.getCameraEntity() != camera) mc.setCameraEntity(camera); // z. B. nach F5 o. Ä.
        Options o = mc.options;
        double forward = (o.keyUp.isDown() ? 1 : 0) - (o.keyDown.isDown() ? 1 : 0);
        double strafe = (o.keyLeft.isDown() ? 1 : 0) - (o.keyRight.isDown() ? 1 : 0);
        double up = (o.keyJump.isDown() ? 1 : 0) - (o.keyShift.isDown() ? 1 : 0);
        if (mc.gui.screen() != null) forward = strafe = up = 0;
        double speed = 0.5 * m.horizontalSpeed() * (o.keySprint.isDown() ? 2 : 1);
        double len = Math.sqrt(forward * forward + strafe * strafe);
        if (len > 1) {
            forward /= len;
            strafe /= len;
        }
        double yaw = Math.toRadians(camera.getYRot());
        double dx = (-Math.sin(yaw) * forward + Math.cos(yaw) * strafe) * speed;
        double dz = (Math.cos(yaw) * forward + Math.sin(yaw) * strafe) * speed;
        double dy = up * 0.5 * m.verticalSpeed() * (o.keySprint.isDown() ? 2 : 1);
        camera.setOldPosAndRot();
        camera.setPos(camera.getX() + dx, camera.getY() + dy, camera.getZ() + dz);
    }
}
