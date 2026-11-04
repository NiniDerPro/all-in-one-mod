package dev.aio.fabric;

import dev.aio.core.platform.ItemRef;
import dev.aio.core.platform.Renderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

/** {@link Renderer} über dem {@link DrawContext} von 1.21.1. Pro Frame per {@link #begin} gesetzt. */
public final class FabricRenderer implements Renderer {
    private final MinecraftClient client;
    private DrawContext ctx;

    public FabricRenderer(MinecraftClient client) {
        this.client = client;
    }

    public void begin(DrawContext context) {
        this.ctx = context;
    }

    private TextRenderer font() { return client.textRenderer; }

    @Override public int screenWidth() { return ctx.getScaledWindowWidth(); }

    @Override public int screenHeight() { return ctx.getScaledWindowHeight(); }

    @Override public int textWidth(String text) { return font().getWidth(text); }

    @Override public int fontHeight() { return font().fontHeight; }

    @Override public void text(String text, float x, float y, int argb, boolean shadow) {
        if (text.isEmpty() || (argb >>> 24) == 0) return;
        MatrixStack m = ctx.getMatrices();
        m.push();
        m.translate(x, y, 0);
        ctx.drawText(font(), text, 0, 0, argb, shadow);
        m.pop();
    }

    @Override public void fill(float x1, float y1, float x2, float y2, int argb) {
        if ((argb >>> 24) == 0) return;
        ctx.fill(Math.round(x1), Math.round(y1), Math.round(x2), Math.round(y2), argb);
    }

    @Override public void item(ItemRef item, float x, float y, boolean overlay) {
        if (!(item instanceof FabricItemRef ref)) return;
        MatrixStack m = ctx.getMatrices();
        m.push();
        m.translate(x, y, 0);
        ctx.drawItem(ref.stack(), 0, 0);
        if (overlay) ctx.drawItemInSlot(font(), ref.stack(), 0, 0);
        m.pop();
    }

    @Override public void push() { ctx.getMatrices().push(); }

    @Override public void pop() { ctx.getMatrices().pop(); }

    @Override public void translate(float x, float y) { ctx.getMatrices().translate(x, y, 0); }

    @Override public void scale(float factor) { ctx.getMatrices().scale(factor, factor, 1); }

    @Override public void enableScissor(int x1, int y1, int x2, int y2) { ctx.enableScissor(x1, y1, x2, y2); }

    @Override public void disableScissor() { ctx.disableScissor(); }
}
