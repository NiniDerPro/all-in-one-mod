package dev.aio.fabric;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

/** Shulker contents as a 9×3 grid in the tooltip. */
final class ShulkerTooltip {
    private ShulkerTooltip() {}

    /** Data side (delivered by {@code ItemStack.getTooltipImage}). */
    record Data(NonNullList<ItemStack> items, int color) implements TooltipComponent {}

    /** Zeichen-Seite. */
    static final class Client implements ClientTooltipComponent {
        private static final int SLOT = 18;
        private final Data data;

        Client(Data data) { this.data = data; }

        private int rows() { return Math.max(1, (data.items().size() + 8) / 9); }

        @Override public int getHeight() { return rows() * SLOT + 6; }

        @Override public int getWidth(Font font) { return 9 * SLOT + 4; }

        @Override public void renderImage(Font font, int x, int y, GuiGraphics g) {
            int totalWidth = getWidth(font), totalHeight = getHeight();
            int w = 9 * SLOT + 2, h = rows() * SLOT + 2;
            int bg = data.color() == 0 ? 0xFF8B8B8B : 0xFF000000 | data.color();
            g.fill(x, y, x + w, y + h, 0xFF373737);
            g.fill(x + 1, y + 1, x + w - 1, y + h - 1, darken(bg));
            for (int i = 0; i < data.items().size(); i++) {
                int sx = x + 1 + (i % 9) * SLOT, sy = y + 1 + (i / 9) * SLOT;
                g.fill(sx + 1, sy + 1, sx + SLOT - 1, sy + SLOT - 1, 0x40000000);
                ItemStack stack = data.items().get(i);
                if (stack.isEmpty()) continue;
                g.renderItem(stack, sx + 1, sy + 1);
                g.renderItemDecorations(font, stack, sx + 1, sy + 1);
            }
        }

        private static int darken(int c) {
            int r = (c >> 16 & 0xFF) * 3 / 4, gr = (c >> 8 & 0xFF) * 3 / 4, b = (c & 0xFF) * 3 / 4;
            return 0xFF000000 | r << 16 | gr << 8 | b;
        }
    }
}
