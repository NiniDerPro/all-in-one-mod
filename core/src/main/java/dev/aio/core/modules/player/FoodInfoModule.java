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

    private final BooleanSetting tooltip;
    private final BooleanSetting saturation;
    private final BooleanSetting exhaustion;
    private final BooleanSetting preview;
    private final BooleanSetting healthPreview;
    private final BooleanSetting values;

    public FoodInfoModule() {
        super("foodinfo", "Food Info", "Food values in tooltips, saturation and exhaustion on the hunger bar, preview when holding food",
                Category.COMBAT, false);
        tooltip = add(new BooleanSetting("tooltip", "Food values in tooltip", true));
        saturation = add(new BooleanSetting("saturation", "Show saturation", true));
        exhaustion = add(new BooleanSetting("exhaustion", "Show exhaustion (singleplayer only)", true));
        preview = add(new BooleanSetting("preview", "Preview when holding food", true));
        healthPreview = add(new BooleanSetting("healthPreview", "Preview healed hearts", true));
        values = add(new BooleanSetting("values", "Values as numbers above the hunger bar", false));
    }


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
        // Erschöpfung als heller Balken hinter den Symbolen, von rechts wachsend
        if (w >= 1) {
            r.fill(right - w, top + 1, right, top + 8, 0x70FFFFFF);
            r.fill(right - w, top + 8, right, top + 9, 0x40FFFFFF);
        }
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
        if (values.on()) {
            String text = "Hunger " + s.food() + " · Sat. " + String.format(java.util.Locale.GERMANY, "%.1f", sat)
                    + (Float.isFinite(s.exhaustion()) ? " · Exh. " + String.format(java.util.Locale.GERMANY, "%.2f", s.exhaustion()) : "");
            r.text(text, right - r.textWidth(text), top - 10, 0xFFE0E0E0, true);
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

    /**
     * Goldene Keule (9×9, eigene Pixelgrafik) für Sättigung: '#' Rand, 'o' Gold, 'h' Glanz, 'w' Knochen.
     * Halbe Sättigung zeigt nur die rechte Hälfte, wie halbe Hunger-Symbole.
     */
    private static final String[] SHANK = {
            ".....###.",
            "....#hoo#",
            "...#hooo#",
            "..#ooooo#",
            "..#oooo#.",
            ".##ooo#..",
            "#w#ooo#..",
            "#ww###...",
            ".##......"};

    private static void drawShank(Renderer r, float x, float y, boolean half, float alpha) {
        int a = Math.round(Math.max(0, Math.min(1, alpha)) * 255) << 24;
        for (int row = 0; row < 9; row++) {
            String line = SHANK[row];
            for (int col = half ? 4 : 0; col < 9; col++) {
                int rgb = switch (line.charAt(col)) {
                    case '#' -> 0x6B4A00;
                    case 'o' -> 0xF2B807;
                    case 'h' -> 0xFFE57A;
                    case 'w' -> 0xFFF6D8;
                    default -> -1;
                };
                if (rgb >= 0) r.fill(x + col, y + row, x + col + 1, y + row + 1, a | rgb);
            }
        }
    }

    /** Sättigung von {@code from} bis {@code to} als goldene Keulen über den Hunger-Symbolen. */
    private static void drawSaturation(Renderer r, float right, float top, float from, float to, float alpha) {
        for (int i = 0; i < 10; i++) {
            float lo = i * 2f, hi = lo + 2f;
            float end = Math.min(hi, to), start = Math.max(lo, from);
            if (end - start <= 0) continue;
            drawShank(r, right - i * 8 - 9, top, end - start <= 1, alpha); // 1 Punkt = halbe Keule
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
