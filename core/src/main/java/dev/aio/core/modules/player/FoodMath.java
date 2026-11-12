package dev.aio.core.modules.player;

/** Hunger calculations like in Minecraft (FoodData), without Minecraft dependency. */
public final class FoodMath {
    public static final float MAX_EXHAUSTION = 4f;
    public static final int MAX_FOOD = 20;

    private FoodMath() {}

    /** Hunger after eating (vanilla: capped at 20). */
    public static int foodAfter(int food, int nutrition) { return Math.min(MAX_FOOD, food + nutrition); }

    /** Saturation after eating (vanilla: never above the hunger value). */
    public static float saturationAfter(int food, float saturation, int nutrition, float gain) {
        return Math.min(saturation + gain, foodAfter(food, nutrition));
    }

    /**
     * How much health natural healing still brings from hunger and saturation (estimate).
     * With full hunger and saturation Minecraft heals fast (saturation/6 per step), from 18
     * hunger slowly (1 health for 6 exhaustion). Exhaustion first uses up saturation, then hunger.
     */
    public static float healthIncrement(int food, float saturation, float exhaustion) {
        if (!Float.isFinite(exhaustion)) exhaustion = 0;
        if (!Float.isFinite(saturation)) return 0;
        float health = 0;
        for (int guard = 0; guard < 10_000 && food >= 18; guard++) {
            while (exhaustion >= MAX_EXHAUSTION) {
                exhaustion -= MAX_EXHAUSTION;
                if (saturation > 0) saturation = Math.max(saturation - 1, 0);
                else food = Math.max(food - 1, 0);
            }
            if (food < 18) break;
            if (food >= MAX_FOOD && saturation > 0) {
                float spent = Math.min(saturation, 6f);
                health += spent / 6f;
                exhaustion += spent;
            } else {
                health += 1;
                exhaustion += 6;
            }
        }
        return health;
    }
}
