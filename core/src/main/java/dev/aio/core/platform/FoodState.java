package dev.aio.core.platform;

/**
 * Hunger state of the player for the food info.
 *
 * @param exhaustion   exhaustion 0..4, NaN if unknown (servers do not send it)
 * @param heldNutrition nutrition of the food in hand, 0 = no food
 * @param heldCanEat   whether it could be eaten right now (full? always edible?)
 * @param visible      whether the hunger bar is visible (not in creative mode, not on mounts)
 */
public record FoodState(int food, float saturation, float exhaustion, float health, float maxHealth, float absorption,
                        int heldNutrition, float heldSaturation, boolean heldCanEat, boolean visible) {
    public static final FoodState NONE = new FoodState(0, 0, Float.NaN, 0, 20, 0, 0, 0, false, false);
}
