package dev.aio.fabric;

import dev.aio.core.platform.ItemRef;
import dev.aio.core.platform.Renderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2fStack;

/** {@link Renderer} über dem {@link GuiGraphicsExtractor} von 26.3. Pro Frame per {@link #begin} gesetzt. */
public final class FabricRenderer implements Renderer {
    private final Minecraft client;
    private GuiGraphicsExtractor g;

    public FabricRenderer(Minecraft client) {
        this.client = client;
    }

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

    @Override public void fill(float x1, float y1, float x2, float y2, int argb) {
        if ((argb >>> 24) == 0) return;
        g.fill(Math.round(x1), Math.round(y1), Math.round(x2), Math.round(y2), argb);
    }

    @Override public void item(ItemRef item, float x, float y, boolean overlay) {
        if (!(item instanceof FabricItemRef ref)) return;
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        g.item(ref.stack(), 0, 0);
        if (overlay) g.itemDecorations(font(), ref.stack(), 0, 0);
        pose.popMatrix();
    }

    @Override public void push() { g.pose().pushMatrix(); }

    @Override public void pop() { g.pose().popMatrix(); }

    @Override public void translate(float x, float y) { g.pose().translate(x, y); }

    @Override public void scale(float factor) { g.pose().scale(factor); }

    @Override public void enableScissor(int x1, int y1, int x2, int y2) { g.enableScissor(x1, y1, x2, y2); }

    @Override public void disableScissor() { g.disableScissor(); }
}
