package dev.aio.core.hud;

import dev.aio.core.platform.Renderer;

/** Something that can be selected, moved and resized in the HUD editor. */
public interface HudTarget {
    String name();

    /** The module the target belongs to (for settings via right click). */
    HudElement owner();

    float screenX(Renderer r);

    float screenY(Renderer r);

    float scaledWidth(Renderer r);

    float scaledHeight(Renderer r);

    float scaleX();

    float scaleY();

    void setScreenPos(Renderer r, float x, float y);

    void setScreenBounds(Renderer r, float x, float y, float w, float h);

    void resetLayout();
}
