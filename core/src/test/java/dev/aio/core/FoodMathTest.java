package dev.aio.core;

import dev.aio.core.modules.player.FoodMath;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FoodMathTest {
    @Test void eatingIsCappedLikeVanilla() {
        assertEquals(20, FoodMath.foodAfter(17, 6));
        assertEquals(12f, FoodMath.saturationAfter(6, 2f, 6, 14.4f), 1e-4, "Sättigung nie über dem Hungerwert");
    }

    @Test void healthEstimate() {
        assertEquals(0f, FoodMath.healthIncrement(17, 5f, 0f), "unter 18 Hunger kein Heilen");
        // voller Hunger mit 6 Sättigung: schnelles Heilen bringt mindestens ein Leben
        assertTrue(FoodMath.healthIncrement(20, 6f, 0f) >= 1f);
        // mehr Sättigung heilt mehr
        assertTrue(FoodMath.healthIncrement(20, 12f, 0f) > FoodMath.healthIncrement(20, 3f, 0f));
        assertTrue(Float.isFinite(FoodMath.healthIncrement(20, 20f, Float.NaN)));
    }
}
