package dev.aio.core.hud;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.platform.Renderer;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.ui.Draw;
import dev.aio.core.setting.ColorSetting;
import dev.aio.core.setting.EnumSetting;
import dev.aio.core.setting.NumberSetting;
import dev.aio.core.setting.Setting;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared base of all HUD elements: position, scale, colour, background, shadow, visibility.
 *
 * <p>The position is stored as a fraction 0..1 of the free space
 * ({@code x * (screen width - element width)}). An element at the right edge therefore stays
 * at the right edge even with a different resolution or GUI scale.
 */
public abstract class HudElement extends Module implements HudTarget {
    public static final int PADDING = 4;
    public static final double MIN_SCALE = 0.25;
    public static final double MAX_SCALE = 5.0;

    protected final NumberSetting posX;
    protected final NumberSetting posY;
    protected final NumberSetting scale;
    protected final NumberSetting scaleY;
    protected final EnumSetting<ColorMode> colorMode;
    protected final ColorSetting textColor;
    protected final ColorSetting secondColor;
    protected final NumberSetting colorSpeed;
    protected final BooleanSetting background;
    protected final ColorSetting backgroundColor;
    protected final BooleanSetting shadow;
    /** Whether the element already got a place (automatically when first switched on). */
    protected final BooleanSetting placed;

    protected HudElement(String id, String name, String description, double defaultX, double defaultY) {
        this(id, name, description, Category.HUD, false, defaultX, defaultY);
    }

    protected HudElement(String id, String name, String description, Category category, boolean gameplayAffecting,
                         double defaultX, double defaultY) {
        super(id, name, description, category, gameplayAffecting);
        posX = add(new NumberSetting("x", "X", defaultX, 0, 1, 0));
        posX.hidden();
        posY = add(new NumberSetting("y", "Y", defaultY, 0, 1, 0));
        posY.hidden();
        // Width and height separate, so that in the HUD editor you can drag freely at every edge/corner.
        // The id "scale" stays for old configs (it was the shared size there).
        scale = add(new NumberSetting("scale", "Width", 1.0, MIN_SCALE, MAX_SCALE, 0.01, "x"));
        scaleY = add(new NumberSetting("scaleY", "Height", 1.0, MIN_SCALE, MAX_SCALE, 0.01, "x"));
        colorMode = add(new EnumSetting<>("colorMode", "Color mode", defaultColorMode()));
        textColor = add(new ColorSetting("textColor", "Text color", defaultTextColor()));
        textColor.visibleWhen(() -> colorMode.get() != ColorMode.CHROMA);
        secondColor = add(new ColorSetting("secondColor", "Second color", 0xFF4FC3FF));
        secondColor.visibleWhen(() -> colorMode.get() == ColorMode.GRADIENT);
        colorSpeed = add(new NumberSetting("colorSpeed", "Color speed", 0.4, 0.05, 2, 0.05, "x"));
        colorSpeed.visibleWhen(() -> colorMode.get() != ColorMode.STATIC);
        background = add(new BooleanSetting("background", "Background", defaultBackground()));
        backgroundColor = add(new ColorSetting("backgroundColor", "Background color", defaultBackgroundColor()));
        backgroundColor.visibleWhen(background::on);
        shadow = add(new BooleanSetting("shadow", "Text shadow", true));
        placed = add(new BooleanSetting("placed", "Placed", false));
        placed.hidden();
    }

    // ---- Individually movable parts ----

    private final List<HudPart> parts = new ArrayList<>();
    private BooleanSetting separate;

    /**
     * Adds an individually movable part (call in the constructor of the subclass).
     * The first part creates the setting "Move parts separately".
     */
    protected final HudPart addPart(String partId, String partName) {
        if (separate == null) separate = add(new BooleanSetting("separate", "Move parts separately", true));
        String p = "part." + partId + ".";
        NumberSetting x = add(new NumberSetting(p + "x", partName + " X", 0, 0, 1, 0));
        NumberSetting y = add(new NumberSetting(p + "y", partName + " Y", 0, 0, 1, 0));
        NumberSetting sx = add(new NumberSetting(p + "scale", partName + " width", 1.0, MIN_SCALE, MAX_SCALE, 0.01, "x"));
        NumberSetting sy = add(new NumberSetting(p + "scaleY", partName + " height", 1.0, MIN_SCALE, MAX_SCALE, 0.01, "x"));
        BooleanSetting pl = add(new BooleanSetting(p + "placed", partName + " placed", false));
        x.hidden();
        y.hidden();
        sx.hidden();
        sy.hidden();
        pl.hidden();
        HudPart part = new HudPart(this, partId, partName, x, y, sx, sy, pl);
        parts.add(part);
        return part;
    }

    public List<HudPart> parts() { return parts; }

    /** Whether the parts are currently separate (otherwise one connected block). */
    public boolean separated() { return separate != null && separate.on() && !parts.isEmpty(); }

    /** Parts that are currently shown (all in the editor). */
    public List<HudPart> visibleParts(boolean preview) {
        List<HudPart> out = new ArrayList<>();
        for (HudPart p : parts) if (partEnabled(p) && (preview || partHasContent(p))) out.add(p);
        return out;
    }

    /** Whether a part is shown at all by setting (e.g. "Show date" off). */
    protected boolean partEnabled(HudPart part) { return true; }

    protected boolean partHasContent(HudPart part) { return true; }

    protected int partWidth(Renderer r, HudPart part) { return width(r); }

    protected int partHeight(Renderer r, HudPart part) { return height(r); }

    /** Position of the part in the connected element (unscaled), start place when separating. */
    protected float[] partOffset(Renderer r, HudPart part) { return new float[]{0, 0}; }

    /** Draws a part in the local coordinate system (0,0 = top left corner of the part). */
    protected void renderPart(Renderer r, HudPart part, boolean preview) {}

    /** For elements that draw freely across the screen (markers, nametags): hide box settings. */
    protected final void hideBoxSettings() {
        for (var s : new Setting<?>[]{posX, posY, scale, scaleY, colorMode, textColor, secondColor,
                colorSpeed, background, backgroundColor, shadow}) {
            s.hidden();
        }
    }

    /** Default colour mode. Queried in the constructor, so it may only return a constant. */
    protected ColorMode defaultColorMode() { return ColorMode.CLEAN; }

    /** Default text colour (ARGB). As above: only return a constant. */
    protected int defaultTextColor() { return 0xFFFFFFFF; }

    /** Whether the background is on by default. As above: only return a constant. */
    protected boolean defaultBackground() { return true; }

    /** Default background colour (ARGB). As above: only return a constant. */
    protected int defaultBackgroundColor() { return DEFAULT_BACKGROUND; }

    /** Neuer Standard-Hintergrund: dunkles Graphit, leicht durchsichtig. */
    public static final int DEFAULT_BACKGROUND = 0x8C0E1015;

    /** Once on the switch to the new design: old default values (rainbow, grey) take over the new style. */
    public void migrateStyle() {
        if (colorMode.get() == ColorMode.CHROMA && defaultColorMode() != ColorMode.CHROMA) colorMode.reset();
        if (backgroundColor.argb() == 0x6F000000) backgroundColor.reset();
    }

    // ---- Content (from subclasses) ----

    /** Width without scaling, including padding. */
    public abstract int width(Renderer r);

    /** Height without scaling, including padding. */
    public abstract int height(Renderer r);

    /**
     * Draws the content in the local coordinate system (0,0 = top left corner, unscaled).
     *
     * @param preview true in the HUD editor: show placeholders when there is nothing to display
     */
    protected abstract void renderContent(Renderer r, boolean preview);

    /** Draws freely across the whole screen (e.g. waypoint markers). Default: nothing. */
    public void renderOverlay(Renderer r, float partialTick) {}

    /** Whether the element can be moved in the HUD editor. */
    public boolean editable() { return true; }

    /** Called every tick while the element is active. Ideal for updating caches. */
    public void update() {}

    /** Whether the element has something to show in game right now (e.g. potion effects only with effects). */
    public boolean hasContent() { return true; }

    /** Whether the default background box should be drawn. Keystrokes draw their own, for example. */
    protected boolean drawsDefaultBackground() { return true; }

    // ---- Zeichnen ----

    public final void render(Renderer r, boolean preview) {
        if (separated()) {
            for (HudPart part : visibleParts(preview)) {
                r.push();
                r.translate(part.screenX(r), part.screenY(r));
                r.scale(part.scaleX(), part.scaleY());
                if (background.on() && drawsDefaultBackground()) Draw.round(r, 0, 0, part.width(r), part.height(r), 2, backgroundColor.argb());
                renderPart(r, part, preview);
                r.pop();
            }
            return;
        }
        if (!preview && !hasContent()) return;
        float x = screenX(r);
        float y = screenY(r);
        r.push();
        r.translate(x, y);
        r.scale(scaleX(), scaleY());
        if (background.on() && drawsDefaultBackground()) Draw.round(r, 0, 0, width(r), height(r), 2, backgroundColor.argb());
        renderContent(r, preview);
        r.pop();
    }

    // ---- Position (Bildschirmpixel, skaliert) ----

    @Override public HudElement owner() { return this; }

    public float scaleX() { return scale.floatValue(); }

    public float scaleY() { return scaleY.floatValue(); }

    public float scaledWidth(Renderer r) { return width(r) * scaleX(); }

    public float scaledHeight(Renderer r) { return height(r) * scaleY(); }

    /**
     * Sets the element to a rectangle in screen pixels (HUD editor). Width and height are
     * limited to the allowed range.
     */
    public void setScreenBounds(Renderer r, float x, float y, float w, float h) {
        int bw = Math.max(1, width(r)), bh = Math.max(1, height(r));
        scale.set(clampScale(w / bw));
        scaleY.set(clampScale(h / bh));
        setScreenPos(r, x, y);
    }

    /** Reset position and size to the default values. */
    public void resetLayout() {
        scale.reset();
        scaleY.reset();
        posX.reset();
        posY.reset();
        for (HudPart p : parts) p.resetLayout();
    }

    static double clampScale(double v) { return Math.max(MIN_SCALE, Math.min(MAX_SCALE, v)); }

    /** Distance to the screen edge, so that nothing sticks directly to the edge. */
    public static final float EDGE = 3;

    /** Screen position from the stored fraction (0 = left edge, 1 = right edge). */
    static float toScreen(double fraction, float screen, float size) { return toScreen(fraction, screen, size, EDGE); }

    static double toFraction(float pos, float screen, float size) { return toFraction(pos, screen, size, EDGE); }

    static float toScreen(double fraction, float screen, float size, float edge) {
        float free = screen - size - 2 * edge;
        return free <= 0 ? Math.max(0, (screen - size) / 2f) : edge + (float) (fraction * free);
    }

    static double toFraction(float pos, float screen, float size, float edge) {
        float free = screen - size - 2 * edge;
        if (free <= 0) return 0;
        // Dragged all the way to the screen edge: snap to the edge (even if the margin is negative)
        if (pos <= Math.max(edge, 0)) return 0;
        if (pos >= screen - size - Math.max(edge, 0)) return 1;
        return clamp01((pos - edge) / free);
    }

    /**
     * Distance to the screen edge at position 0 or 1. Negative means: the (invisible) padding
     * may extend over the edge so that the content itself touches the edge.
     */
    protected float edgeMargin() { return EDGE; }

    /** Like {@link #edgeMargin()}, for individually moved parts. */
    protected float partEdgeMargin() { return EDGE; }

    public float screenX(Renderer r) { return toScreen(posX.get(), r.screenWidth(), scaledWidth(r), edgeMargin()); }

    public float screenY(Renderer r) { return toScreen(posY.get(), r.screenHeight(), scaledHeight(r), edgeMargin()); }

    public void setScreenPos(Renderer r, float x, float y) {
        posX.set(toFraction(x, r.screenWidth(), scaledWidth(r), edgeMargin()));
        posY.set(toFraction(y, r.screenHeight(), scaledHeight(r), edgeMargin()));
    }

    public NumberSetting scaleSetting() { return scale; }

    public boolean isPlaced() { return placed.on(); }

    public void markPlaced() { placed.set(true); }

    protected int textColor() { return textColor.argb(); }

    /** Draws text in the chosen colour mode (fixed, rainbow, gradient). */
    protected void drawText(Renderer r, String text, float x, float y) {
        ColoredText.draw(r, text, x, y, colorMode.get(), textColor.argb(), secondColor.argb(),
                colorSpeed.floatValue(), shadow.on(), (float) (posX.get() * 300 + posY.get() * 120));
    }

    /** Dynamic colour for areas in the chosen colour mode. */
    protected int dynamicColor(float offset) {
        return ColoredText.color(colorMode.get(), textColor.argb(), secondColor.argb(), colorSpeed.floatValue(),
                (float) (posX.get() * 300 + posY.get() * 120) + offset);
    }

    public ColorMode colorMode() { return colorMode.get(); }

    protected boolean shadow() { return shadow.on(); }

    private static double clamp01(double v) { return Math.max(0, Math.min(1, v)); }
}
