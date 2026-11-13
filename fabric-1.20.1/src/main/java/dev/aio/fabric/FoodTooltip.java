package dev.aio.fabric;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Nutrition in the tooltip:
 * hunger icons on top, below them the golden saturation icons. More than 10 icons are
 * shown as one with "x12".
 */
public final class FoodTooltip {
    private FoodTooltip() {}

    private static final ResourceLocation ICONS = new ResourceLocation("aio", "textures/foodinfo/icons.png");
    private static final ResourceLocation FOOD_EMPTY = new ResourceLocation("hud/food_empty");
    private static final ResourceLocation FOOD_HALF = new ResourceLocation("hud/food_half");
    private static final ResourceLocation FOOD_FULL = new ResourceLocation("hud/food_full");
    private static final ResourceLocation FOOD_HALF_HUNGER = new ResourceLocation("hud/food_half_hunger");
    private static final ResourceLocation FOOD_FULL_HUNGER = new ResourceLocation("hud/food_full_hunger");

    /** Tooltip data for food or null. */
    public static TooltipComponent create(ItemStack stack) {
        FoodProperties food = stack.getItem().getFoodProperties();
        if (food == null || food.getNutrition() == 0 && (food.getNutrition() * food.getSaturationModifier() * 2f) == 0) return null;
        return new Data(food.getNutrition(), (food.getNutrition() * food.getSaturationModifier() * 2f), FoodHud.isRotten(stack));
    }

    record Data(int nutrition, float saturation, boolean rotten) implements TooltipComponent {}

    static final class Client implements ClientTooltipComponent {
        private final Data data;
        private int hungerBars, saturationBars;
        private String hungerText, saturationText;

        Client(Data data) {
            this.data = data;
            hungerBars = (int) Math.ceil(Math.abs(data.nutrition()) / 2f);
            if (hungerBars > 10) {
                hungerText = "x" + ((data.nutrition() < 0 ? -1 : 1) * hungerBars);
                hungerBars = 1;
            }
            saturationBars = (int) Math.ceil(Math.abs(data.saturation()) / 2f);
            if (saturationBars > 10 || saturationBars == 0) {
                saturationText = "x" + ((data.saturation() < 0 ? -1 : 1) * saturationBars);
                saturationBars = 1;
            }
        }

        @Override public int getHeight() { return 9 + 1 + 7 + 3; }

        @Override public int getWidth(Font font) {
            int hunger = hungerBars * 9 + (hungerText != null ? font.width(hungerText) : 0);
            int sat = saturationBars * 7 + (saturationText != null ? font.width(saturationText) : 0);
            return Math.max(hunger, sat);
        }

        @Override public void renderImage(Font font, int tx, int ty, GuiGraphics g) {
            int totalWidth = getWidth(font), totalHeight = getHeight();
            int nutrition = data.nutrition();
            // from right to left so that the drumsticks point in the right direction
            int x = tx + (hungerBars - 1) * 9, y = ty;
            for (int i = 0; i < hungerBars * 2; i += 2) {
                GuiCompat.sprite(g, FOOD_EMPTY, x, y, 9, 9);
                if (nutrition > i) {
                    boolean half = nutrition - 1 == i;
                    ResourceLocation icon = half ? (data.rotten() ? FOOD_HALF_HUNGER : FOOD_HALF) : (data.rotten() ? FOOD_FULL_HUNGER : FOOD_FULL);
                    GuiCompat.sprite(g, icon, x, y, 9, 9);
                }
                x -= 9;
            }
            if (hungerText != null) smallText(g, font, hungerText, x + 18, y, 2);

            float sat = data.saturation(), abs = Math.abs(sat);
            x = tx + (saturationBars - 1) * 7;
            y = ty + 10;
            for (int i = 0; i < saturationBars * 2; i += 2) {
                float bar = (abs - i) / 2f;
                boolean faded = abs <= i;
                int u = bar >= 1 ? 21 : bar > 0.5 ? 14 : bar > 0.25 ? 7 : bar > 0 ? 0 : 28;
                int color = faded ? 0x80FFFFFF : 0xFFFFFFFF;
                GuiCompat.blit(g, ICONS, x, y, u, sat >= 0 ? 27 : 34, 7, 7, 256, 256, color);
                x -= 7;
            }
            if (saturationText != null) smallText(g, font, saturationText, x + 14, y, 1);
        }

        private static void smallText(GuiGraphics g, Font font, String text, int x, int y, int dy) {
            PoseStack pose = g.pose();
            pose.pushPose();
            pose.translate(x, y, 0);
            pose.scale(0.75f, 0.75f, 1f);
            g.drawString(font, text, 2, dy, 0xFFAAAAAA);
            pose.popPose();
        }
    }
}
