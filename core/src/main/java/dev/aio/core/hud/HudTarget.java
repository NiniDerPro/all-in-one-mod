package dev.aio.core.hud;

import dev.aio.core.platform.Renderer;

/** Etwas, das im HUD-Editor ausgewählt, verschoben und in der Größe gezogen werden kann. */
public interface HudTarget {
    String name();

    /** Das Modul, zu dem das Ziel gehört (für Einstellungen per Rechtsklick). */
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
