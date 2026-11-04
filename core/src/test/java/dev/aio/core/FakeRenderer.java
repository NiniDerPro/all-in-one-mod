package dev.aio.core;

import dev.aio.core.platform.ItemRef;
import dev.aio.core.platform.Renderer;

/** Renderer, der nur zählt: 6 px pro Zeichen, Schrifthöhe 9. */
public class FakeRenderer implements Renderer {
    public int width = 480, height = 270;
    public int textCalls, fillCalls, widthCalls;
    public int depth;

    @Override public int screenWidth() { return width; }
    @Override public int screenHeight() { return height; }
    @Override public int textWidth(String text) { widthCalls++; return text.length() * 6; }
    @Override public int fontHeight() { return 9; }
    @Override public void text(String text, float x, float y, int argb, boolean shadow) { textCalls++; }
    @Override public void fill(float x1, float y1, float x2, float y2, int argb) { fillCalls++; }
    @Override public void item(ItemRef item, float x, float y, boolean overlay) {}
    @Override public void push() { depth++; }
    @Override public void pop() { depth--; }
    @Override public void translate(float x, float y) {}
    @Override public void scale(float factor) {}
    @Override public void enableScissor(int x1, int y1, int x2, int y2) {}
    @Override public void disableScissor() {}
}
