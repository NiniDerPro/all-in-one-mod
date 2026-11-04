package dev.aio.core.modules.player;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.platform.FoodState;
import dev.aio.core.platform.Renderer;
import dev.aio.core.setting.BooleanSetting;

/**
 * Essens-Info: Nährwert und Sättigung im Tooltip, Sättigung und Erschöpfung auf der Hungerleiste
 * und beim Halten von Essen eine Vorschau, wie viel Hunger, Sättigung und Leben es bringt.
 */
public final class FoodInfoModule extends Module {
    private static final String FOOD_FULL = "minecraft:hud/food_full";
    private static final String FOOD_HALF = "minecraft:hud/food_half";
    private static final String HEART_FULL = "minecraft:hud/heart/full";
    private static final String HEART_HALF = "minecraft:hud/heart/half";
    private static final int SATURATION_COLOR = 0xFFFFC21F;

    private final BooleanSetting tooltip;
    private final BooleanSetting saturation;
    private final BooleanSetting exhaustion;
    private final BooleanSetting preview;
    private final BooleanSetting healthPreview;

    public FoodInfoModule() {
        super("foodinfo", "Essens-Info", "Nährwert im Tooltip, Sättigung/Erschöpfung auf der Hungerleiste, Vorschau beim Essen",
                Category.COMBAT, false);
        tooltip = add(new BooleanSetting("tooltip", "Nährwert im Tooltip", true));
        saturation = add(new BooleanSetting("saturation", "Sättigung anzeigen", true));
        exhaustion = add(new BooleanSetting("exhaustion", "Erschöpfung anzeigen (nur Einzelspieler)", true));
        preview = add(new BooleanSetting("preview", "Vorschau beim Halten von Essen", true));
        healthPreview = add(new BooleanSetting("healthPreview", "Vorschau geheilter Herzen", true));
    }

    @Override public boolean enabledByDefault() { return true; }

    public boolean showTooltip() { return isActive() && tooltip.on(); }

    /** Blinken der Vorschau wie bei AppleSkin-ähnlichen Anzeigen: sanft pulsierend. */
    private static float pulse() {
        double t = (System.currentTimeMillis() % 1500) / 1500.0;
        return (float) (0.35 + 0.45 * (0.5 + 0.5 * Math.sin(t * Math.PI * 2)));
    }

    private static int clamp2(int v) { return Math.max(0, Math.min(2, v)); }

    private static int alpha(float a) { return (Math.round(Math.max(0, Math.min(1, a)) * 255) << 24) | 0xFFFFFF; }

    /** Unter der Hungerleiste: Erschöpfung als Balken von rechts. */
    public void renderUnderFood(Renderer r, FoodState s, float right, float top) {
        if (!isActive() || !s.visible() || !exhaustion.on() || !Float.isFinite(s.exhaustion())) return;
        float ratio = Math.max(0, Math.min(1, s.exhaustion() / FoodMath.MAX_EXHAUSTION));
        float w = ratio * 81;
        if (w >= 1) r.fill(right - w, top, right, top + 9, 0x60FFFFFF);
    }

    /** Über der Hungerleiste: Sättigung als goldene Rahmen und die Vorschau des gehaltenen Essens. */
    public void renderOverFood(Renderer r, FoodState s, float right, float top) {
        if (!isActive() || !s.visible()) return;
        boolean holding = preview.on() && s.heldNutrition() > 0 && s.heldCanEat();
        float sat = s.saturation();
        float newSat = holding ? FoodMath.saturationAfter(s.food(), sat, s.heldNutrition(), s.heldSaturation()) : sat;
        if (saturation.on()) {
            drawSaturation(r, right, top, 0, sat, 1f);
            if (holding && newSat > sat) drawSaturation(r, right, top, sat, newSat, pulse());
        }
        if (holding) {
            int newFood = FoodMath.foodAfter(s.food(), s.heldNutrition());
            int a = alpha(pulse());
            for (int i = 0; i < 10; i++) {
                int before = clamp2(s.food() - i * 2), after = clamp2(newFood - i * 2);
                if (after > before) r.sprite(after == 2 ? FOOD_FULL : FOOD_HALF, right - i * 8 - 9, top, 9, 9, a);
            }
        }
    }

    /** Sättigung von {@code from} bis {@code to} als Rahmen um die Hunger-Symbole. */
    private static void drawSaturation(Renderer r, float right, float top, float from, float to, float alpha) {
        int color = (Math.round(alpha * 255) << 24) | (SATURATION_COLOR & 0xFFFFFF);
        for (int i = 0; i < 10; i++) {
            float lo = i * 2f, hi = lo + 2f;
            float amount = Math.min(hi, to) - Math.max(lo, from);
            if (amount <= 0) continue;
            float x = right - i * 8 - 9;
            boolean full = Math.min(hi, to) >= hi && Math.max(lo, from) <= lo;
            // voller Rahmen für 2 Punkte, sonst nur die rechte Hälfte (wie halbe Symbole)
            float x1 = full ? x : x + 4;
            r.fill(x1, top, x + 9, top + 1, color);
            r.fill(x1, top + 8, x + 9, top + 9, color);
            r.fill(x + 8, top, x + 9, top + 9, color);
            if (full) r.fill(x, top, x + 1, top + 9, color);
        }
    }

    /** Über der Lebensleiste: Herzen, die das Essen über natürliches Heilen bringen würde. */
    public void renderOverHealth(Renderer r, FoodState s, float left, float top) {
        if (!isActive() || !s.visible() || !preview.on() || !healthPreview.on()) return;
        if (s.heldNutrition() <= 0 || !s.heldCanEat()) return;
        if (s.maxHealth() + s.absorption() > 20) return; // mehrere Herzreihen: nicht sauber vorhersagbar
        int newFood = FoodMath.foodAfter(s.food(), s.heldNutrition());
        float newSat = FoodMath.saturationAfter(s.food(), s.saturation(), s.heldNutrition(), s.heldSaturation());
        float gain = FoodMath.healthIncrement(newFood, newSat, s.exhaustion());
        float target = Math.min(s.maxHealth(), s.health() + gain);
        int from = (int) Math.ceil(s.health()), to = (int) Math.floor(target);
        if (to <= from) return;
        int a = alpha(pulse());
        for (int i = 0; i < 10; i++) {
            int before = clamp2(from - i * 2), after = clamp2(to - i * 2);
            if (after > before) r.sprite(after == 2 ? HEART_FULL : HEART_HALF, left + i * 8, top, 9, 9, a);
        }
    }
}
