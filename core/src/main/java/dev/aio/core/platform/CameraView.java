package dev.aio.core.platform;

/**
 * Kamera für Projektionen. Yaw/Pitch in MC-Konvention, {@code fov} vertikal in Grad.
 * {@code bobWalk}/{@code bob}: Laufwackeln wie im Spiel (0 = keins), damit Marker mitwackeln.
 */
public record CameraView(double x, double y, double z, float yaw, float pitch, double fov, float bobWalk, float bob) {
    public CameraView(double x, double y, double z, float yaw, float pitch, double fov) {
        this(x, y, z, yaw, pitch, fov, 0, 0);
    }
}
