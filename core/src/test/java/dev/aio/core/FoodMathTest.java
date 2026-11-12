package dev.aio.core;

import dev.aio.core.modules.player.FoodMath;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FoodMathTest {
    @Test void eatingIsCappedLikeVanilla() {
        assertEquals(20, FoodMath.foodAfter(17, 6));
        assertEquals(12f, FoodMath.saturationAfter(6, 2f, 6, 14.4f), 1e-4, "saturation never above the hunger value");
    }

    @Test void healthEstimate() {
        assertEquals(0f, FoodMath.healthIncrement(17, 5f, 0f), "no healing below 18 hunger");
        // full hunger with 6 saturation: fast healing brings at least one health
        assertTrue(FoodMath.healthIncrement(20, 6f, 0f) >= 1f);
        // more saturation heals more
        assertTrue(FoodMath.healthIncrement(20, 12f, 0f) > FoodMath.healthIncrement(20, 3f, 0f));
        assertTrue(Float.isFinite(FoodMath.healthIncrement(20, 20f, Float.NaN)));
    }
}
