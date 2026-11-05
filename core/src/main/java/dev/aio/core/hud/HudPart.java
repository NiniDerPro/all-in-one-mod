package dev.aio.core.hud;

import dev.aio.core.platform.Renderer;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.NumberSetting;

/**
 * Einzeln verschiebbarer Teil eines HUD-Elements (z. B. Uhrzeit und Datum, oder ein Rüstungsteil).
 * Eigene Position und Größe, gespeichert als versteckte Einstellungen des Elements. Bis man einen
 * Teil zum ersten Mal verschiebt, sitzt er dort, wo er im zusammenhängenden Element säße.
 */
public final class HudPart implements HudTarget {
    private final HudElement owner;
    private final String id;
    private final String name;
    final NumberSetting posX, posY, scale, scaleY;
    final BooleanSetting placed;

    HudPart(HudElement owner, String id, String name, NumberSetting posX, NumberSetting posY,
            NumberSetting scale, NumberSetting scaleY, BooleanSetting placed) {
        this.owner = owner;
        this.id = id;
        this.name = name;
        this.posX = posX;
        this.posY = posY;
        this.scale = scale;
        this.scaleY = scaleY;
        this.placed = placed;
    }

    public String id() { return id; }

    @Override public String name() { return owner.name() + ": " + name; }

    @Override public HudElement owner() { return owner; }

    public int width(Renderer r) { return Math.max(1, owner.partWidth(r, this)); }

    public int height(Renderer r) { return Math.max(1, owner.partHeight(r, this)); }

    /** Noch nie einzeln bewegt: an die Stelle im zusammenhängenden Element setzen. */
    void ensurePlaced(Renderer r) {
        if (placed.on()) return;
        float[] off = owner.partOffset(r, this);
        scale.set((double) owner.scaleX());
        scaleY.set((double) owner.scaleY());
        setScreenPos(r, owner.screenX(r) + off[0] * owner.scaleX(), owner.screenY(r) + off[1] * owner.scaleY());
        placed.set(true);
    }

    @Override public float scaleX() { return scale.floatValue(); }

    @Override public float scaleY() { return scaleY.floatValue(); }

    @Override public float scaledWidth(Renderer r) { return width(r) * scaleX(); }

    @Override public float scaledHeight(Renderer r) { return height(r) * scaleY(); }

    @Override public float screenX(Renderer r) {
        ensurePlaced(r);
        return HudElement.toScreen(posX.get(), r.screenWidth(), scaledWidth(r));
    }

    @Override public float screenY(Renderer r) {
        ensurePlaced(r);
        return HudElement.toScreen(posY.get(), r.screenHeight(), scaledHeight(r));
    }

    @Override public void setScreenPos(Renderer r, float x, float y) {
        posX.set(HudElement.toFraction(x, r.screenWidth(), scaledWidth(r)));
        posY.set(HudElement.toFraction(y, r.screenHeight(), scaledHeight(r)));
    }

    @Override public void setScreenBounds(Renderer r, float x, float y, float w, float h) {
        scale.set(HudElement.clampScale(w / width(r)));
        scaleY.set(HudElement.clampScale(h / height(r)));
        setScreenPos(r, x, y);
    }

    /** Zurück an die Stelle im zusammenhängenden Element. */
    @Override public void resetLayout() { placed.set(false); }
}
