package dev.aio.fabric;

import dev.aio.core.platform.CameraView;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** Camera of the current frame, for markers in the HUD that should sit exactly on the world. */
final class FrameCamera {
    private FrameCamera() {}

    /**
     * The camera with which the game draws this frame. In 26.3 the camera is determined first,
     * then the HUD, and only afterwards the world is drawn: the camera state at the HUD
     * is therefore already that of this frame. The matrix is built exactly like when drawing the world
     * (projection × view bobbing × viewing direction), so markers stay fixed on their point.
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
