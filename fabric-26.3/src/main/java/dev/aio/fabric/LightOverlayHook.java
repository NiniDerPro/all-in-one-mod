package dev.aio.fabric;

/** Öffentlicher Einstieg für den Mixin (LightOverlay selbst bleibt paketintern). */
public final class LightOverlayHook {
    private LightOverlayHook() {}

    public static void sectionDirty(int sx, int sy, int sz) { LightOverlay.onSectionDirty(sx, sy, sz); }
}
