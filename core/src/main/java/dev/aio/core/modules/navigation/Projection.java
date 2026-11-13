package dev.aio.core.modules.navigation;

import dev.aio.core.platform.CameraView;

/**
 * Projects world points onto the screen (perspective like the MC camera).
 * MC convention: yaw 0 = looking towards +Z, positive pitch = looking down, FOV is vertical.
 */
public final class Projection {
    /** Screen position; {@code onScreen} false if the point is behind the camera or outside. */
    public record Point(float x, float y, boolean onScreen, boolean behind) {}

    private Projection() {}

    /** Height of one font pixel of a vanilla nametag in blocks (0.025 per pixel). */
    private static final double TAG_PIXEL = 0.025;

    /**
     * Size factor for overlays above entities, so they keep their size relative to the world like
     * vanilla nametags: smaller far away, larger when zoomed in. Never larger than 1 (normal GUI size)
     * so tags right in front of the camera do not cover the picture.
     */
    public static float worldScale(double distance, CameraView cam, float screenH) {
        double tanHalf = Math.tan(Math.toRadians(Math.max(1, cam.fov())) / 2);
        double pixelsPerBlock = screenH / (2 * tanHalf * Math.max(distance, 0.1));
        return (float) Math.min(1.0, TAG_PIXEL * pixelsPerBlock);
    }

    /** With the camera including view bobbing (how Minecraft shifts and tilts the picture when walking). */
    public static Point project(double px, double py, double pz, CameraView cam, float screenW, float screenH) {
        float[] m = cam.viewProj();
        if (m == null) return project(px, py, pz, cam.x(), cam.y(), cam.z(), cam.yaw(), cam.pitch(), cam.fov(), screenW, screenH);
        // Exactly like the game: clip = M · (p − camera); column-major matrix
        double dx = px - cam.x(), dy = py - cam.y(), dz = pz - cam.z();
        double cx = m[0] * dx + m[4] * dy + m[8] * dz + m[12];
        double cy = m[1] * dx + m[5] * dy + m[9] * dz + m[13];
        double cw = m[3] * dx + m[7] * dy + m[11] * dz + m[15];
        boolean behind = cw <= 0.05;
        double w = Math.max(Math.abs(cw), 0.05);
        double ndcX = cx / w, ndcY = cy / w;
        if (behind) {
            ndcX = -ndcX * 1000;
            ndcY = -ndcY * 1000;
        }
        float sx = (float) (screenW / 2 * (1 + ndcX));
        float sy = (float) (screenH / 2 * (1 - ndcY));
        boolean on = !behind && sx >= 0 && sx <= screenW && sy >= 0 && sy <= screenH;
        return new Point(sx, sy, on, behind);
    }

    public static Point project(double px, double py, double pz, double ex, double ey, double ez,
                                float yawDeg, float pitchDeg, double fovDeg, float screenW, float screenH) {
        double yaw = Math.toRadians(yawDeg), pitch = Math.toRadians(pitchDeg);
        // Blickrichtung
        double fx = -Math.sin(yaw) * Math.cos(pitch), fy = -Math.sin(pitch), fz = Math.cos(yaw) * Math.cos(pitch);
        // Right (screen) and up
        double rx = -Math.cos(yaw), ry = 0, rz = -Math.sin(yaw);
        double ux = ry * fz - rz * fy, uy = rz * fx - rx * fz, uz = rx * fy - ry * fx;

        double dx = px - ex, dy = py - ey, dz = pz - ez;
        double xc = dx * rx + dy * ry + dz * rz;
        double yc = dx * ux + dy * uy + dz * uz;
        double zc = dx * fx + dy * fy + dz * fz;

        double t = Math.tan(Math.toRadians(fovDeg) / 2);
        double aspect = screenW / Math.max(1f, screenH);
        boolean behind = zc <= 0.05;
        double z = behind ? Math.max(Math.abs(zc), 0.05) : zc;
        double ndcX = xc / (z * t * aspect);
        double ndcY = yc / (z * t);
        if (behind) {
            // behind the camera: mirror the direction so that edge markers end up on the right side
            ndcX = -ndcX * 1000;
            ndcY = -ndcY * 1000;
        }
        float sx = (float) (screenW / 2 * (1 + ndcX));
        float sy = (float) (screenH / 2 * (1 - ndcY));
        boolean on = !behind && sx >= 0 && sx <= screenW && sy >= 0 && sy <= screenH;
        return new Point(sx, sy, on, behind);
    }
}
