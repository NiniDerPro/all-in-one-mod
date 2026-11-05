package dev.aio.core.modules.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NametagScaleTest {
    @Test void fullSizeUpCloseAndSmallerFurtherAway() {
        assertEquals(1f, NametagsModule.distanceScale(2));
        assertEquals(1f, NametagsModule.distanceScale(4));
        float at8 = NametagsModule.distanceScale(8), at16 = NametagsModule.distanceScale(16), at32 = NametagsModule.distanceScale(32);
        assertTrue(at8 < 0.75f && at8 > 0.55f, "8 Blöcke: etwa zwei Drittel, war " + at8);
        assertTrue(at16 < at8 && at32 <= at16, "nimmt mit der Entfernung ab");
        assertEquals(0.5f, NametagsModule.distanceScale(200), 1e-6, "nie kleiner als die Hälfte (sonst unleserlich)");
    }

    private static NametagsModule.Placed at(float x, float y, float w) {
        return new NametagsModule.Placed(null, x, y, 1, x - w / 2, y - 10, x + w / 2, y);
    }

    @Test void farTagsHiddenBehindNearOnesAreDropped() {
        var near = at(100, 100, 60);
        var behind = at(105, 102, 60);   // fast deckungsgleich, weiter weg
        var beside = at(200, 100, 60);   // daneben, bleibt
        var kept = NametagsModule.declutter(java.util.List.of(near, behind, beside));
        assertEquals(java.util.List.of(near, beside), kept);
    }
}
