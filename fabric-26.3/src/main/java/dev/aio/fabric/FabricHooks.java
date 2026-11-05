package dev.aio.fabric;

/** Kleiner gemeinsamer Zustand zwischen Mixins und Plattform. */
public final class FabricHooks {
    /** Ob Minecraft gerade Profiler-Daten für das Kreisdiagramm sammelt. */
    public static volatile boolean profilerResults;

    private FabricHooks() {}

    /** Wird gesetzt, sobald der Freelook-Kamera-Hook einmal ausgeführt wurde (= greift in dieser Version). */
    public static volatile boolean cameraHookSeen;

    /** Zuletzt berechnetes Welt-Sichtfeld (inkl. Sprinten, Effekten, Zoom), 0 = noch unbekannt. */
    public static volatile float lastFov;
}
