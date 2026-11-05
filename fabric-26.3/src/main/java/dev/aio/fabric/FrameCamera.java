package dev.aio.fabric;

import dev.aio.core.platform.CameraView;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** Kamera des aktuellen Frames, für Marker im HUD, die exakt auf der Welt sitzen sollen. */
final class FrameCamera {
    private FrameCamera() {}

    /**
     * Die Kamera, mit der das Spiel diesen Frame zeichnet. In 26.3 wird erst die Kamera
     * ermittelt, dann das HUD, und erst danach die Welt gezeichnet: Der Kamera-Zustand ist beim HUD
     * also schon der dieses Frames. Die Matrix wird genau wie beim Zeichnen der Welt gebaut
     * (Projektion × Laufwackeln × Blickrichtung), daher bleiben Marker fest auf ihrem Punkt.
     */
    static CameraView current(Minecraft mc, double fov) {
        try {
            var c = mc.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
            if (c == null || !c.initialized || c.pos == null || c.projectionMatrix == null || c.viewRotationMatrix == null) return null;
            Matrix4f m = new Matrix4f(c.projectionMatrix);
            var e = c.entityRenderState;
            if (e != null && e.isPlayer && mc.options.bobView().get()) {
                float f = e.backwardsInterpolatedWalkDistance, g = e.bob;
                m.translate(Mth.sin(f * (float) Math.PI) * g * 0.5f,
                        -Math.abs(Mth.cos(f * (float) Math.PI) * g), 0);
                m.rotateZ((float) Math.toRadians(Mth.sin(f * (float) Math.PI) * g * 3f));
                m.rotateX((float) Math.toRadians(Math.abs(Mth.cos(f * (float) Math.PI - 0.2f) * g) * 5f));
            }
            m.mul(c.viewRotationMatrix);
            float[] arr = new float[16];
            m.get(arr);
            return new CameraView(c.pos.x, c.pos.y, c.pos.z, c.yRot, c.xRot, fov, arr);
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
