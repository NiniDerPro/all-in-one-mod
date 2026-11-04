package dev.aio.fabric;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/** Nährwert (Hunger-Symbole) und Sättigung im Tooltip von Essen. */
public final class FoodTooltip {
    private FoodTooltip() {}

    /** Tooltip-Daten für Essen oder null. */
    public static TooltipComponent create(ItemStack stack) {
        FoodProperties food = stack.get(DataComponents.FOOD);
        return food == null ? null : new Data(food.nutrition(), food.saturation());
    }

    private static final Identifier FULL = Identifier.withDefaultNamespace("hud/food_full");
    private static final Identifier HALF = Identifier.withDefaultNamespace("hud/food_half");
    private static final Identifier EMPTY = Identifier.withDefaultNamespace("hud/food_empty");

    record Data(int nutrition, float saturation) implements TooltipComponent {}

    static final class Client implements ClientTooltipComponent {
        private final Data data;

        Client(Data data) { this.data = data; }

        private int icons() { return Math.max(1, (data.nutrition() + 1) / 2); }

        private String saturationText() {
            return String.format(Locale.ROOT, "Saturation +%.1f", data.saturation());
        }

        @Override public int getHeight(Font font) { return 22; }

        @Override public int getWidth(Font font) {
            return Math.max(Math.min(icons(), 10) * 8 + 2, font.width(saturationText()));
        }

        @Override public void extractImage(Font font, int x, int y, int totalWidth, int totalHeight, GuiGraphicsExtractor g) {
            int n = Math.min(icons(), 10);
            for (int i = 0; i < n; i++) {
                int ix = x + i * 8;
                g.blitSprite(RenderPipelines.GUI_TEXTURED, EMPTY, ix, y, 9, 9);
                boolean half = i == n - 1 && data.nutrition() % 2 == 1;
                g.blitSprite(RenderPipelines.GUI_TEXTURED, half ? HALF : FULL, ix, y, 9, 9);
            }
            if (icons() > 10) g.text(font, "+" + (icons() - 10), x + n * 8 + 2, y + 1, 0xFFFFFFFF, true);
            g.text(font, saturationText(), x, y + 11, 0xFFFFC21F, true);
        }
    }
}
