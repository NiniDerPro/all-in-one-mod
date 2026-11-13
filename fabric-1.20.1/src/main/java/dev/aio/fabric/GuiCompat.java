package dev.aio.fabric;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Tinted texture drawing for this Minecraft version (it tints through a global colour instead of a parameter). */
final class GuiCompat {
    private GuiCompat() {}

    private static void tint(GuiGraphics g, int argb) {
        g.setColor(((argb >> 16) & 0xFF) / 255f, ((argb >> 8) & 0xFF) / 255f, (argb & 0xFF) / 255f, ((argb >>> 24) & 0xFF) / 255f);
    }

    static void blit(GuiGraphics g, ResourceLocation texture, int x, int y, float u, float v, int w, int h, int texW, int texH, int argb) {
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        tint(g, argb);
        g.blit(texture, x, y, u, v, w, h, texW, texH);
        g.setColor(1f, 1f, 1f, 1f);
    }

    /** This Minecraft version keeps the HUD icons in one sheet instead of single sprites. */
    private static final ResourceLocation VANILLA_ICONS = new ResourceLocation("textures/gui/icons.png");

    /** Position of a HUD icon in the sheet, by the sprite name newer versions use. */
    private static int[] uv(ResourceLocation sprite) {
        return switch (sprite.getPath()) {
            case "hud/heart/container" -> new int[]{16, 0};
            case "hud/heart/full" -> new int[]{52, 0};
            case "hud/heart/half" -> new int[]{61, 0};
            case "hud/heart/container_hardcore" -> new int[]{16, 45};
            case "hud/heart/hardcore_full" -> new int[]{52, 45};
            case "hud/heart/hardcore_half" -> new int[]{61, 45};
            case "hud/food_empty" -> new int[]{16, 27};
            case "hud/food_full" -> new int[]{52, 27};
            case "hud/food_half" -> new int[]{61, 27};
            case "hud/food_empty_hunger" -> new int[]{133, 27};
            case "hud/food_full_hunger" -> new int[]{88, 27};
            case "hud/food_half_hunger" -> new int[]{97, 27};
            default -> null;
        };
    }

    static void sprite(GuiGraphics g, ResourceLocation sprite, int x, int y, int w, int h, int argb) {
        int[] uv = uv(sprite);
        if (uv == null) return;
        blit(g, VANILLA_ICONS, x, y, uv[0], uv[1], w, h, 256, 256, argb);
    }

    static void sprite(GuiGraphics g, ResourceLocation sprite, int x, int y, int w, int h) {
        sprite(g, sprite, x, y, w, h, 0xFFFFFFFF);
    }
}
