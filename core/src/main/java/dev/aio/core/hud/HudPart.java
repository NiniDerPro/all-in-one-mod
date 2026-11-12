package dev.aio.core.hud;

import dev.aio.core.platform.Renderer;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.NumberSetting;

/**
 * Individually movable part of a HUD element (e.g. time and date, or an armor part).
 * Own position and size, stored as hidden settings of the element. Until a
 * part is moved for the first time, it sits where it would sit in the connected element.
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

    /** Never moved individually: put it at its place in the connected element. */
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
        return HudElement.toScreen(posX.get(), r.screenWidth(), scaledWidth(r), owner.partEdgeMargin());
    }

    @Override public float screenY(Renderer r) {
        ensurePlaced(r);
        return HudElement.toScreen(posY.get(), r.screenHeight(), scaledHeight(r), owner.partEdgeMargin());
    }

    @Override public void setScreenPos(Renderer r, float x, float y) {
        posX.set(HudElement.toFraction(x, r.screenWidth(), scaledWidth(r), owner.partEdgeMargin()));
        posY.set(HudElement.toFraction(y, r.screenHeight(), scaledHeight(r), owner.partEdgeMargin()));
    }

    /** If the part is almost at the left edge, put it right at the edge. */
    public void snapLeft(double threshold) {
        if (placed.on() && posX.get() <= threshold) posX.set(0.0);
    }

    @Override public void setScreenBounds(Renderer r, float x, float y, float w, float h) {
        scale.set(HudElement.clampScale(w / width(r)));
        scaleY.set(HudElement.clampScale(h / height(r)));
        setScreenPos(r, x, y);
    }

    /** Back to the place in the connected element. */
    @Override public void resetLayout() { placed.set(false); }
}
