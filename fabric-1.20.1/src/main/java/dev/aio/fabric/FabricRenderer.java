package dev.aio.fabric;

import dev.aio.core.platform.ItemRef;
import dev.aio.core.platform.Renderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

/** {@link Renderer} over the {@link GuiGraphics} of 26.3. Set per frame via {@link #begin}. */
public final class FabricRenderer implements Renderer {
    private final Minecraft client;
    private GuiGraphics g;

    public FabricRenderer(Minecraft client) {
        this.client = client;
    }

    /** For vanilla drawing functions (e.g. the profiler chart). */
    public GuiGraphics graphics() { return g; }

    public void begin(GuiGraphics graphics) {
        this.g = graphics;
    }

    private Font font() { return client.font; }

    @Override public int screenWidth() { return g.guiWidth(); }

    @Override public int screenHeight() { return g.guiHeight(); }

    @Override public int textWidth(String text) { return font().width(text); }

    @Override public int fontHeight() { return font().lineHeight; }

    @Override public void text(String text, float x, float y, int argb, boolean shadow) {
        if (text.isEmpty() || (argb >>> 24) == 0) return;
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        g.drawString(font(), text, 0, 0, argb, shadow);
        pose.popPose();
    }

    @Override public void coloredText(String text, int[] colors, float[] offsets, float x, float y, boolean shadow) {
        if (text.isEmpty() || colors.length == 0 || (colors[0] >>> 24) == 0) return;
        FormattedCharSequence seq = sink -> {
            for (int i = 0, idx = 0; i < text.length(); idx++) {
                int cp = text.codePointAt(i);
                int c = colors[Math.min(idx, colors.length - 1)];
                if (!sink.accept(i, Style.EMPTY.withColor(c & 0xFFFFFF), cp)) return false;
                i += Character.charCount(cp);
            }
            return true;
        };
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        g.drawString(font(), seq, 0, 0, colors[0] | 0x00FFFFFF, shadow);
        pose.popPose();
    }

    @Override public void fill(float x1, float y1, float x2, float y2, int argb) {
        if ((argb >>> 24) == 0) return;
        g.fill(Math.round(x1), Math.round(y1), Math.round(x2), Math.round(y2), argb);
    }

    @Override public void item(ItemRef item, float x, float y, boolean overlay) {
        if (!(item instanceof FabricItemRef ref)) return;
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        g.renderItem(ref.stack(), 0, 0);
        if (overlay) g.renderItemDecorations(font(), ref.stack(), 0, 0);
        pose.popPose();
    }

    private record Img(net.minecraft.client.renderer.texture.DynamicTexture tex, ResourceLocation id, int version, int w, int h) {}

    private final Map<String, Img> images = new HashMap<>();

    @Override public void image(String key, int[] argb, int w, int h, int version, float x, float y, float dw, float dh) {
        Img img = images.get(key);
        if (img == null || img.w != w || img.h != h) {
            if (img != null) client.getTextureManager().release(img.id);
            var tex = new net.minecraft.client.renderer.texture.DynamicTexture(w, h, false);
            ResourceLocation id = new ResourceLocation("aio", "dyn/" + key);
            client.getTextureManager().register(id, tex);
            img = new Img(tex, id, -1, w, h);
            images.put(key, img);
        }
        if (img.version != version) {
            var px = img.tex.getPixels();
            for (int j = 0; j < h; j++) {
                for (int i = 0; i < w; i++) {
                    int c = argb[j * w + i];
                    px.setPixelRGBA(i, j, (c & 0xFF00FF00) | ((c >> 16) & 0xFF) | ((c & 0xFF) << 16));
                }
            }
            img.tex.upload();
            img = new Img(img.tex, img.id, version, w, h);
            images.put(key, img);
        }
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(dw / w, dh / h, 1f);
        g.blit(img.id, 0, 0, 0f, 0f, w, h, w, h);
        pose.popPose();
    }

    private final Map<String, ResourceLocation> sprites = new HashMap<>();

    @Override public void sprite(String id, float x, float y, int width, int height) {
        // GUI atlas sprites are not available through GuiGraphics on this Minecraft version
    }

    @Override public void sprite(String id, float x, float y, int width, int height, int argb) {
        // see above
    }

    @Override public void push() { g.pose().pushPose(); }

    @Override public void pop() { g.pose().popPose(); }

    @Override public void translate(float x, float y) { g.pose().translate(x, y, 0); }

    @Override public void scale(float factor) { g.pose().scale(factor, factor, 1f); }

    @Override public void scale(float sx, float sy) { g.pose().scale(sx, sy, 1f); }

    @Override public void enableScissor(int x1, int y1, int x2, int y2) { g.enableScissor(x1, y1, x2, y2); }

    @Override public void disableScissor() { g.disableScissor(); }
}
