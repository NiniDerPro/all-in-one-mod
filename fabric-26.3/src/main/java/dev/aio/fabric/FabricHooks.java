package dev.aio.fabric;

/** Small shared state between mixins and platform. */
public final class FabricHooks {
    /** Whether Minecraft is currently collecting profiler data for the pie chart. */
    public static volatile boolean profilerResults;

    private FabricHooks() {}

    /** Set as soon as the freelook camera hook has run once (= applies in this version). */
    public static volatile boolean cameraHookSeen;

    /** Last computed world field of view (incl. sprinting, effects, zoom), 0 = not known yet. */
    public static volatile float lastFov;
}
