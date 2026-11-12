package dev.aio.core.ui;

import dev.aio.core.platform.Renderer;

/**
 * Version-independent screen. The adapter wraps it in an MC {@code Screen} and
 * passes drawing and input through 1:1. Coordinates are scaled GUI pixels.
 */
public interface UiScreen {
    void render(Renderer r, int mouseX, int mouseY, float delta);

    default boolean mouseClicked(double x, double y, int button) { return false; }

    default boolean mouseReleased(double x, double y, int button) { return false; }

    default boolean mouseDragged(double x, double y, int button, double dx, double dy) { return false; }

    default boolean mouseScrolled(double x, double y, double amount) { return false; }

    /** @param key MC key name. @return true if the key was consumed. Escape otherwise closes the screen. */
    default boolean keyPressed(String key) { return false; }

    default boolean charTyped(String text) { return false; }

    default void onClose() {}

    /** Called on every display, also when returning to this screen. */
    default void onOpen() {}

    /** Whether the screen returns to the previous screen on close (e.g. recipes → inventory). */
    default boolean returnsToPrevious() { return false; }

    /**
     * Does this screen replace a vanilla screen (e.g. the key bindings)? Then closing leads
     * back to where the replaced screen would have led.
     */
    default boolean replacesParent() { return false; }

    default String title() { return "AIO"; }
}
