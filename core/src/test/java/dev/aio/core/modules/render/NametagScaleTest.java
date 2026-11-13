package dev.aio.core.modules.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NametagScaleTest {
    private static final dev.aio.core.platform.CameraView CAM = new dev.aio.core.platform.CameraView(0, 0, 0, 0, 0, 70);

    private static float scale(double d, dev.aio.core.platform.CameraView cam) {
        return dev.aio.core.modules.navigation.Projection.worldScale(d, cam, 270);
    }

    @Test void keepsWorldSizeLikeVanillaNametags() {
        assertEquals(1f, scale(2, CAM), "never larger than normal up close");
        float at8 = scale(8, CAM), at16 = scale(16, CAM), at64 = scale(64, CAM);
        assertEquals(at8 / 2, at16, 1e-4, "twice as far, half as big (like the player model)");
        assertEquals(at16 / 4, at64, 1e-4);
        assertTrue(at64 < 0.1f, "far away it is small, not half size, was " + at64);
        var zoomed = new dev.aio.core.platform.CameraView(0, 0, 0, 0, 0, 20);
        assertTrue(scale(64, zoomed) > at64 * 3, "grows when zoomed in, like the world");
    }

    private static NametagsModule.Placed at(float x, float y, float w) {
        return new NametagsModule.Placed(null, x, y, 1, x - w / 2, y - 10, x + w / 2, y);
    }

    @Test void farTagsHiddenBehindNearOnesAreDropped() {
        var near = at(100, 100, 60);
        var behind = at(105, 102, 60);   // fast deckungsgleich, weiter weg
        var beside = at(200, 100, 60);   // next to it, stays
        var kept = NametagsModule.declutter(java.util.List.of(near, behind, beside));
        assertEquals(java.util.List.of(near, beside), kept);
    }
}
