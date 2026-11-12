package dev.aio.fabric;

/** Small shared state between mixins and platform. */
public final class FabricHooks {
    /** Whether Minecraft is currently collecting profiler data for the pie chart. */
    public static volatile boolean profilerResults;

    private FabricHooks() {}

    /** Set as soon as the freelook camera hook has run once (= applies in this version). */
    public static volatile boolean cameraHookSeen;

    /** Zuletzt berechnetes Welt-Sichtfeld (inkl. Sprinten, Effekten, Zoom), 0 = noch unbekannt. */
    public static volatile float lastFov;
}
