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

    static void sprite(GuiGraphics g, ResourceLocation sprite, int x, int y, int w, int h, int argb) {
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        tint(g, argb);
        g.blitSprite(sprite, x, y, w, h);
        g.setColor(1f, 1f, 1f, 1f);
    }

    static void sprite(GuiGraphics g, ResourceLocation sprite, int x, int y, int w, int h) {
        g.blitSprite(sprite, x, y, w, h);
    }
}
