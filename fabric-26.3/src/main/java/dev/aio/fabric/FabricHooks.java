package dev.aio.fabric;

/** Kleiner gemeinsamer Zustand zwischen Mixins und Plattform. */
public final class FabricHooks {
    private FabricHooks() {}

    /** Wird gesetzt, sobald der Freelook-Kamera-Hook einmal ausgeführt wurde (= greift in dieser Version). */
    public static volatile boolean cameraHookSeen;

    /** Zuletzt berechnetes Welt-Sichtfeld (inkl. Sprinten, Effekten, Zoom), 0 = noch unbekannt. */
    public static volatile float lastFov;
}
