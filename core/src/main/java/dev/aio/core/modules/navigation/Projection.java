package dev.aio.core.modules.navigation;

/**
 * Projiziert Weltpunkte auf den Bildschirm (Perspektive wie die MC-Kamera).
 * MC-Konvention: Yaw 0 = Blick nach +Z, positiver Pitch = Blick nach unten, FOV ist vertikal.
 */
public final class Projection {
    /** Bildschirmposition; {@code onScreen} false, wenn der Punkt hinter der Kamera oder außerhalb liegt. */
    public record Point(float x, float y, boolean onScreen, boolean behind) {}

    private Projection() {}

    /** Mit der Kamera inklusive Laufwackeln (wie Minecraft das Bild beim Gehen verschiebt und kippt). */
    public static Point project(double px, double py, double pz, dev.aio.core.platform.CameraView cam, float screenW, float screenH) {
        return project(px, py, pz, cam.x(), cam.y(), cam.z(), cam.yaw(), cam.pitch(), cam.fov(), screenW, screenH, cam.bobWalk(), cam.bob());
    }

    public static Point project(double px, double py, double pz, double ex, double ey, double ez,
                                float yawDeg, float pitchDeg, double fovDeg, float screenW, float screenH) {
        return project(px, py, pz, ex, ey, ez, yawDeg, pitchDeg, fovDeg, screenW, screenH, 0, 0);
    }

    public static Point project(double px, double py, double pz, double ex, double ey, double ez,
                                float yawDeg, float pitchDeg, double fovDeg, float screenW, float screenH, float bobWalk, float bob) {
        double yaw = Math.toRadians(yawDeg), pitch = Math.toRadians(pitchDeg);
        // Blickrichtung
        double fx = -Math.sin(yaw) * Math.cos(pitch), fy = -Math.sin(pitch), fz = Math.cos(yaw) * Math.cos(pitch);
        // Rechts (Bildschirm) und Oben
        double rx = -Math.cos(yaw), ry = 0, rz = -Math.sin(yaw);
        double ux = ry * fz - rz * fy, uy = rz * fx - rx * fz, uz = rx * fy - ry * fx;

        double dx = px - ex, dy = py - ey, dz = pz - ez;
        double xc = dx * rx + dy * ry + dz * rz;
        double yc = dx * ux + dy * uy + dz * uz;
        double zc = dx * fx + dy * fy + dz * fz;
        if (bob != 0) {
            // Wie Minecraft: verschieben (sin, -|cos|), um die Blickachse kippen (Z), nicken (X).
            // Bildraum: x rechts, y oben, -z nach vorn.
            double f = bobWalk * Math.PI;
            double tx = Math.sin(f) * bob * 0.5, ty = -Math.abs(Math.cos(f) * bob);
            double a = Math.toRadians(Math.sin(f) * bob * 3), b = Math.toRadians(Math.abs(Math.cos(f - 0.2) * bob) * 5);
            double vx = xc, vy = yc, vz = -zc;
            double y1 = vy * Math.cos(b) - vz * Math.sin(b), z1 = vy * Math.sin(b) + vz * Math.cos(b); // um X
            double x2 = vx * Math.cos(a) - y1 * Math.sin(a), y2 = vx * Math.sin(a) + y1 * Math.cos(a); // um Z
            xc = x2 + tx;
            yc = y2 + ty;
            zc = -z1;
        }

        double t = Math.tan(Math.toRadians(fovDeg) / 2);
        double aspect = screenW / Math.max(1f, screenH);
        boolean behind = zc <= 0.05;
        double z = behind ? Math.max(Math.abs(zc), 0.05) : zc;
        double ndcX = xc / (z * t * aspect);
        double ndcY = yc / (z * t);
        if (behind) {
            // hinter der Kamera: Richtung spiegeln, damit Randmarker auf der richtigen Seite landen
            ndcX = -ndcX * 1000;
            ndcY = -ndcY * 1000;
        }
        float sx = (float) (screenW / 2 * (1 + ndcX));
        float sy = (float) (screenH / 2 * (1 - ndcY));
        boolean on = !behind && sx >= 0 && sx <= screenW && sy >= 0 && sy <= screenH;
        return new Point(sx, sy, on, behind);
    }
}
