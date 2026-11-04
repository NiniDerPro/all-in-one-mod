package dev.aio.core.platform;

/** Kamera für Projektionen. Yaw/Pitch in MC-Konvention, {@code fov} vertikal in Grad. */
public record CameraView(double x, double y, double z, float yaw, float pitch, double fov) {}
