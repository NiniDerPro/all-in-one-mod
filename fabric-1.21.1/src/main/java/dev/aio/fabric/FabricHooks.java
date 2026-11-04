package dev.aio.fabric;

/** Kleiner gemeinsamer Zustand für Mixins. */
public final class FabricHooks {
    private FabricHooks() {}

    /** True, während GameOptions gespeichert werden: Fullbright darf den Gamma-Wert dann nicht überschreiben. */
    public static boolean writingOptions;
}
