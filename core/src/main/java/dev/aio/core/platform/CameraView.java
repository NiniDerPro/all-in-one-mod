package dev.aio.core.platform;

/**
 * Kamera für Projektionen. Yaw/Pitch in MC-Konvention, {@code fov} vertikal in Grad.
 * {@code viewProj}: falls vorhanden die exakte Matrix, mit der das Spiel diesen Frame zeichnet
 * (Projektion × Laufwackeln × Drehung, spaltenweise, 16 Werte), relativ zur Kameraposition.
 */
public record CameraView(double x, double y, double z, float yaw, float pitch, double fov, float[] viewProj) {
    public CameraView(double x, double y, double z, float yaw, float pitch, double fov) {
        this(x, y, z, yaw, pitch, fov, null);
    }
}
