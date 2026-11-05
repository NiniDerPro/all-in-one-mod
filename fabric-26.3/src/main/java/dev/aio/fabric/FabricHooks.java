package dev.aio.fabric;

/** Kleiner gemeinsamer Zustand zwischen Mixins und Plattform. */
public final class FabricHooks {
    /** Ob Minecraft gerade Profiler-Daten für das Kreisdiagramm sammelt. */
    public static volatile boolean profilerResults;

    private FabricHooks() {}

    /** Wird gesetzt, sobald der Freelook-Kamera-Hook einmal ausgeführt wurde (= greift in dieser Version). */
    public static volatile boolean cameraHookSeen;

    /** Zuletzt berechnetes Welt-Sichtfeld (inkl. Sprinten, Effekten, Zoom), 0 = noch unbekannt. */
    public static volatile float lastFov;

    // ---- Kamera des aktuellen Frames (für Marker, die exakt auf der Welt sitzen) ----

    /**
     * Die Kamera, mit der das Spiel diesen Frame zeichnet. In 26.3 wird erst die Kamera
     * ermittelt, dann das HUD, und erst danach die Welt gezeichnet: Der Kamera-Zustand ist beim HUD
     * also schon der dieses Frames. Die Matrix wird genau wie beim Zeichnen der Welt gebaut
     * (Projektion × Laufwackeln × Blickrichtung), daher bleiben Marker fest auf ihrem Punkt.
     */
    public static dev.aio.core.platform.CameraView frameCamera(net.minecraft.client.Minecraft mc, double fov) {
        try {
            var c = mc.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
            if (c == null || !c.initialized || c.pos == null || c.projectionMatrix == null || c.viewRotationMatrix == null) return null;
            org.joml.Matrix4f m = new org.joml.Matrix4f(c.projectionMatrix);
            var e = c.entityRenderState;
            if (e != null && e.isPlayer && mc.options.bobView().get()) {
                float f = e.backwardsInterpolatedWalkDistance, g = e.bob;
                m.translate(net.minecraft.util.Mth.sin(f * (float) Math.PI) * g * 0.5f,
                        -Math.abs(net.minecraft.util.Mth.cos(f * (float) Math.PI) * g), 0);
                m.rotateZ((float) Math.toRadians(net.minecraft.util.Mth.sin(f * (float) Math.PI) * g * 3f));
                m.rotateX((float) Math.toRadians(Math.abs(net.minecraft.util.Mth.cos(f * (float) Math.PI - 0.2f) * g) * 5f));
            }
            m.mul(c.viewRotationMatrix);
            float[] arr = new float[16];
            m.get(arr);
            return new dev.aio.core.platform.CameraView(c.pos.x, c.pos.y, c.pos.z, c.yRot, c.xRot, fov, arr);
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
