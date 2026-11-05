package dev.aio.core.modules.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NametagScaleTest {
    @Test void fullSizeUpCloseAndSmallerFurtherAway() {
        assertEquals(1f, NametagsModule.distanceScale(2));
        assertEquals(1f, NametagsModule.distanceScale(4));
        float at8 = NametagsModule.distanceScale(8), at16 = NametagsModule.distanceScale(16), at32 = NametagsModule.distanceScale(32);
        assertTrue(at8 < 0.75f && at8 > 0.55f, "8 Blöcke: etwa zwei Drittel, war " + at8);
        assertTrue(at16 < at8 && at32 < at16, "nimmt mit der Entfernung weiter ab");
        assertEquals(0.25f, NametagsModule.distanceScale(200), 1e-6, "nie kleiner als ein Viertel");
    }
}
