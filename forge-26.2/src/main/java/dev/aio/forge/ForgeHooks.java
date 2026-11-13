package dev.aio.forge;

/** Small shared state between mixins and platform. */
public final class ForgeHooks {
    /** Whether Minecraft is currently collecting profiler data for the pie chart. */
    public static volatile boolean profilerResults;

    private ForgeHooks() {}

    /** Set as soon as the freelook camera hook has run once (= applies in this version). */
    public static volatile boolean cameraHookSeen;

    /** Last computed world field of view (incl. sprinting, effects, zoom), 0 = not known yet. */
    public static volatile float lastFov;
}
