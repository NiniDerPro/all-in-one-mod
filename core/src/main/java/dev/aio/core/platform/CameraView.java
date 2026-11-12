package dev.aio.core.platform;

/**
 * Camera for projections. Yaw/pitch in MC convention, {@code fov} vertical in degrees.
 * {@code viewProj}: if present the exact matrix with which the game draws this frame
 * (Projektion × Laufwackeln × Drehung, spaltenweise, 16 Werte), relativ zur Kameraposition.
 */
public record CameraView(double x, double y, double z, float yaw, float pitch, double fov, float[] viewProj) {
    public CameraView(double x, double y, double z, float yaw, float pitch, double fov) {
        this(x, y, z, yaw, pitch, fov, null);
    }
}
