package dev.aio.core.modules.player;

/** Hunger-Rechnungen wie in Minecraft (FoodData), ohne Minecraft-Abhängigkeit. */
public final class FoodMath {
    public static final float MAX_EXHAUSTION = 4f;
    public static final int MAX_FOOD = 20;

    private FoodMath() {}

    /** Hunger nach dem Essen (Vanilla: auf 20 begrenzt). */
    public static int foodAfter(int food, int nutrition) { return Math.min(MAX_FOOD, food + nutrition); }

    /** Sättigung nach dem Essen (Vanilla: nie über dem Hungerwert). */
    public static float saturationAfter(int food, float saturation, int nutrition, float gain) {
        return Math.min(saturation + gain, foodAfter(food, nutrition));
    }

    /**
     * Wie viel Leben das natürliche Heilen aus Hunger und Sättigung noch bringt (Schätzung).
     * Bei vollem Hunger und Sättigung heilt Minecraft schnell (Sättigung/6 je Schritt), ab 18
     * Hunger langsam (1 Leben für 6 Erschöpfung). Erschöpfung zehrt erst Sättigung, dann Hunger auf.
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
