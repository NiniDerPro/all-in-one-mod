package dev.aio.forge;

import dev.aio.core.platform.ItemRef;
import dev.aio.core.platform.Renderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

/** {@link Renderer} over the {@link GuiGraphicsExtractor} of 26.3. Set per frame via {@link #begin}. */
public final class ForgeRenderer implements Renderer {
    private final Minecraft client;
    private GuiGraphicsExtractor g;

    public ForgeRenderer(Minecraft client) {
        this.client = client;
    }

    /** For vanilla drawing functions (e.g. the profiler chart). */
    public GuiGraphicsExtractor graphics() { return g; }

    public void begin(GuiGraphicsExtractor graphics) {
        this.g = graphics;
    }

    private Font font() { return client.font; }

    @Override public int screenWidth() { return g.guiWidth(); }

    @Override public int screenHeight() { return g.guiHeight(); }

    @Override public int textWidth(String text) { return font().width(text); }

    @Override public int fontHeight() { return font().lineHeight; }

    @Override public void text(String text, float x, float y, int argb, boolean shadow) {
        if (text.isEmpty() || (argb >>> 24) == 0) return;
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        g.text(font(), text, 0, 0, argb, shadow);
        pose.popMatrix();
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
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        g.text(font(), seq, 0, 0, colors[0] | 0x00FFFFFF, shadow);
        pose.popMatrix();
    }

    @Override public void fill(float x1, float y1, float x2, float y2, int argb) {
        if ((argb >>> 24) == 0) return;
        g.fill(Math.round(x1), Math.round(y1), Math.round(x2), Math.round(y2), argb);
    }

    @Override public void item(ItemRef item, float x, float y, boolean overlay) {
        if (!(item instanceof ForgeItemRef ref)) return;
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        g.item(ref.stack(), 0, 0);
        if (overlay) g.itemDecorations(font(), ref.stack(), 0, 0);
        pose.popMatrix();
    }

    private record Img(net.minecraft.client.renderer.texture.DynamicTexture tex, Identifier id, int version, int w, int h) {}

    private final Map<String, Img> images = new HashMap<>();

    @Override public void image(String key, int[] argb, int w, int h, int version, float x, float y, float dw, float dh) {
        Img img = images.get(key);
        if (img == null || img.w != w || img.h != h) {
            if (img != null) client.getTextureManager().release(img.id);
            var tex = new net.minecraft.client.renderer.texture.DynamicTexture(() -> "aio_" + key, w, h, false);
            Identifier id = Identifier.fromNamespaceAndPath("aio", "dyn/" + key);
            client.getTextureManager().register(id, tex);
            img = new Img(tex, id, -1, w, h);
            images.put(key, img);
        }
        if (img.version != version) {
            var px = img.tex.getPixels();
            for (int j = 0; j < h; j++) {
                for (int i = 0; i < w; i++) {
                    int c = argb[j * w + i];
                    px.setPixelABGR(i, j, (c & 0xFF00FF00) | ((c >> 16) & 0xFF) | ((c & 0xFF) << 16));
                }
            }
            img.tex.upload();
            img = new Img(img.tex, img.id, version, w, h);
            images.put(key, img);
        }
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(dw / w, dh / h);
        g.blit(RenderPipelines.GUI_TEXTURED, img.id, 0, 0, 0f, 0f, w, h, w, h);
        pose.popMatrix();
    }

    private final Map<String, Identifier> sprites = new HashMap<>();

    @Override public void sprite(String id, float x, float y, int width, int height) {
        Identifier sprite = sprites.computeIfAbsent(id, Identifier::parse);
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, 0, 0, width, height);
        pose.popMatrix();
    }

    @Override public void sprite(String id, float x, float y, int width, int height, int argb) {
        Identifier sprite = sprites.computeIfAbsent(id, Identifier::parse);
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, 0, 0, width, height, argb);
        pose.popMatrix();
    }

    @Override public void push() { g.pose().pushMatrix(); }

    @Override public void pop() { g.pose().popMatrix(); }

    @Override public void translate(float x, float y) { g.pose().translate(x, y); }

    @Override public void scale(float factor) { g.pose().scale(factor); }

    @Override public void scale(float sx, float sy) { g.pose().scale(sx, sy); }

    @Override public void enableScissor(int x1, int y1, int x2, int y2) { g.enableScissor(x1, y1, x2, y2); }

    @Override public void disableScissor() { g.disableScissor(); }
}
