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
        // 1.21.11 has no render-state objects: markers fall back to the approximate camera
        return null;
    }
}
