package dev.aio.fabric;

/** Public entry point for the mixin (LightOverlay itself stays package-private). */
public final class LightOverlayHook {
    private LightOverlayHook() {}

    public static void sectionDirty(int sx, int sy, int sz) { LightOverlay.onSectionDirty(sx, sy, sz); }
}
