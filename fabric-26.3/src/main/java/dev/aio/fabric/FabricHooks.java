package dev.aio.fabric;

/** Kleiner gemeinsamer Zustand zwischen Mixins und Plattform. */
public final class FabricHooks {
    private FabricHooks() {}

    /** Wird gesetzt, sobald der Freelook-Kamera-Hook einmal ausgeführt wurde (= greift in dieser Version). */
    public static volatile boolean cameraHookSeen;

    /** Zuletzt berechnetes Welt-Sichtfeld (inkl. Sprinten, Effekten, Zoom), 0 = noch unbekannt. */
    public static volatile float lastFov;

    // ---- Kamera des zuletzt gezeichneten Frames (für Marker ohne Zittern) ----
    public static volatile double camX, camY, camZ;
    public static volatile float camYaw, camPitch, camBobWalk, camBob;
    public static volatile long camNanos;

    public static void captureCamera(net.minecraft.client.renderer.state.level.CameraRenderState c) {
        if (c == null || c.pos == null) return;
        camX = c.pos.x;
        camY = c.pos.y;
        camZ = c.pos.z;
        camYaw = c.yRot;
        camPitch = c.xRot;
        var e = c.entityRenderState;
        var mc = net.minecraft.client.Minecraft.getInstance();
        boolean bob = e != null && e.isPlayer && mc.options != null && mc.options.bobView().get();
        camBobWalk = bob ? e.backwardsInterpolatedWalkDistance : 0;
        camBob = bob ? e.bob : 0;
        camNanos = System.nanoTime();
    }
}
