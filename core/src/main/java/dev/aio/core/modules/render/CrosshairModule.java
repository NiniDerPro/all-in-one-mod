package dev.aio.core.modules.render;

import dev.aio.core.hud.HudElement;
import dev.aio.core.module.Category;
import dev.aio.core.platform.Perspective;
import dev.aio.core.platform.Renderer;
import dev.aio.core.platform.Vec3;
import dev.aio.core.setting.ActionSetting;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.ColorSetting;
import dev.aio.core.setting.NumberSetting;
import dev.aio.core.util.Colors;

/**
 * Own crosshair in the style of tactical shooters: centre dot, inner and outer lines each with
 * length, thickness, gap and opacity, outline, colour or rainbow, T shape, spread while moving and
 * an attack indicator. Replaces the vanilla crosshair while active (not with F3 open).
 */
public final class CrosshairModule extends HudElement {
    private final ColorSetting color;
    private final BooleanSetting rainbow;
    private final NumberSetting rainbowSpeed;
    private final BooleanSetting outline;
    private final ColorSetting outlineColor;
    private final NumberSetting outlineThickness;
    private final BooleanSetting dot;
    private final NumberSetting dotSize;
    private final NumberSetting dotOpacity;
    private final BooleanSetting inner;
    private final NumberSetting innerLength, innerThickness, innerGap, innerOpacity;
    private final BooleanSetting outer;
    private final NumberSetting outerLength, outerThickness, outerGap, outerOpacity;
    private final BooleanSetting tShape;
    private final BooleanSetting movementSpread;
    private final NumberSetting spreadAmount;
    private final BooleanSetting attackIndicator;
    private final BooleanSetting thirdPerson;

    private double lastX = Double.NaN, lastZ;
    private long lastNanos;
    private float spread;

    public CrosshairModule() {
        super("crosshair", "Crosshair", "Your own crosshair: dot, lines, gap, outline, colour or rainbow", Category.VISUALS, false, 0, 0);
        // the frame settings of HUD elements (position, background, text colour) make no sense here
        for (var s : settings()) if (s != toggleKey) s.hidden();
        color = add(new ColorSetting("color", "Colour", 0xFFFFFFFF));
        rainbow = add(new BooleanSetting("rainbow", "Rainbow", false));
        rainbowSpeed = add(new NumberSetting("rainbowSpeed", "Rainbow speed", 0.3, 0.05, 2, 0.05));
        rainbowSpeed.visibleWhen(rainbow::on);
        outline = add(new BooleanSetting("outline", "Outline", true));
        outlineColor = add(new ColorSetting("outlineColor", "Outline colour", 0xB4000000));
        outlineColor.visibleWhen(outline::on);
        outlineThickness = add(new NumberSetting("outlineThickness", "Outline thickness", 1, 1, 6, 1, " px"));
        outlineThickness.visibleWhen(outline::on);

        dot = add(new BooleanSetting("dot", "Centre dot", false));
        dotSize = add(new NumberSetting("dotSize", "Dot size", 2, 1, 16, 1, " px"));
        dotSize.visibleWhen(dot::on);
        dotOpacity = add(new NumberSetting("dotOpacity", "Dot opacity", 100, 0, 100, 5, " %"));
        dotOpacity.visibleWhen(dot::on);

        inner = add(new BooleanSetting("inner", "Inner lines", true));
        innerLength = add(new NumberSetting("innerLength", "Inner length", 6, 1, 40, 1, " px"));
        innerThickness = add(new NumberSetting("innerThickness", "Inner thickness", 2, 1, 16, 1, " px"));
        innerGap = add(new NumberSetting("innerGap", "Inner gap", 3, 0, 40, 1, " px"));
        innerOpacity = add(new NumberSetting("innerOpacity", "Inner opacity", 100, 0, 100, 5, " %"));
        for (NumberSetting s : new NumberSetting[]{innerLength, innerThickness, innerGap, innerOpacity}) s.visibleWhen(inner::on);

        outer = add(new BooleanSetting("outer", "Outer lines", false));
        outerLength = add(new NumberSetting("outerLength", "Outer length", 4, 1, 40, 1, " px"));
        outerThickness = add(new NumberSetting("outerThickness", "Outer thickness", 2, 1, 16, 1, " px"));
        outerGap = add(new NumberSetting("outerGap", "Outer gap", 14, 0, 80, 1, " px"));
        outerOpacity = add(new NumberSetting("outerOpacity", "Outer opacity", 50, 0, 100, 5, " %"));
        for (NumberSetting s : new NumberSetting[]{outerLength, outerThickness, outerGap, outerOpacity}) s.visibleWhen(outer::on);

        tShape = add(new BooleanSetting("tShape", "T shape (no top line)", false));
        movementSpread = add(new BooleanSetting("movementSpread", "Spread while moving", false));
        spreadAmount = add(new NumberSetting("spreadAmount", "Spread amount", 8, 1, 40, 1, " px"));
        spreadAmount.visibleWhen(movementSpread::on);
        attackIndicator = add(new BooleanSetting("attackIndicator", "Attack indicator", true));
        thirdPerson = add(new BooleanSetting("thirdPerson", "Show in third person", false));

        add(new ActionSetting("presetClassic", "Preset: classic cross", () -> preset(false, true, 6, 2, 3, false)));
        add(new ActionSetting("presetDot", "Preset: dot only", () -> preset(true, false, 6, 2, 3, false)));
        add(new ActionSetting("presetTactical", "Preset: tactical (dot, short lines, outer lines)", () -> preset(true, true, 4, 2, 4, true)));
    }

    private void preset(boolean withDot, boolean withInner, double length, double thickness, double gap, boolean withOuter) {
        dot.set(withDot);
        dotSize.set(withInner ? 2.0 : 4.0);
        inner.set(withInner);
        innerLength.set(length);
        innerThickness.set(thickness);
        innerGap.set(gap);
        outer.set(withOuter);
        outline.set(true);
        tShape.set(false);
    }

    /** Whether the vanilla crosshair is replaced right now. With F3 open vanilla shows its axis cross. */
    public boolean replacesVanilla() {
        return isActive() && core != null && !platform().debugScreenOpen();
    }

    // ---- HUD element without a frame: drawn in the middle of the screen ----

    @Override public int width(Renderer r) { return 1; }

    @Override public int height(Renderer r) { return 1; }

    @Override protected boolean drawsDefaultBackground() { return false; }

    @Override public boolean editable() { return false; }

    @Override protected void renderContent(Renderer r, boolean preview) {}

    @Override public boolean hasContent() { return platform().inWorld() && !platform().isScreenOpen(); }

    @Override public void renderOverlay(Renderer r, float partialTick) {
        if (!replacesVanilla() || !hasContent()) return;
        if (!thirdPerson.on() && platform().perspective() != Perspective.FIRST_PERSON) return;
        updateSpread();
        // Sizes are real screen pixels: draw in pixel units around the middle pixel, so every edge is sharp
        float scale = (float) Math.max(1, platform().guiScale());
        int pw = Math.round(r.screenWidth() * scale), ph = Math.round(r.screenHeight() * scale);
        r.push();
        r.scale(1f / scale);
        draw(r, pw / 2, ph / 2);
        if (attackIndicator.on()) drawAttack(r, pw / 2, ph / 2, scale);
        r.pop();
    }

    /** Smoothly growing gap while the player moves horizontally. */
    private void updateSpread() {
        long now = System.nanoTime();
        float dt = lastNanos == 0 ? 0 : Math.min(0.1f, (now - lastNanos) / 1e9f);
        lastNanos = now;
        Vec3 p = platform().playerPos();
        double speed = 0;
        if (!Double.isNaN(lastX) && dt > 0) speed = Math.hypot(p.x() - lastX, p.z() - lastZ) / dt;
        lastX = p.x();
        lastZ = p.z();
        float target = movementSpread.on() ? (float) Math.min(1.0, speed / 5.6) : 0f; // 5.6 m/s ≈ sprinting
        spread += (target - spread) * Math.min(1f, dt * 12f);
    }

    /** Draws the crosshair centred at (cx, cy); also used for the preview. */
    public void draw(Renderer r, float cx, float cy) {
        int base = rainbow.on() ? Colors.chroma(0, rainbowSpeed.floatValue(), 255) : color.argb();
        float extra = Math.round(spread * spreadAmount.floatValue());
        if (outer.on()) lines(r, cx, cy, outerLength.floatValue(), outerThickness.floatValue(), outerGap.floatValue() + extra,
                withOpacity(base, outerOpacity.floatValue()));
        if (inner.on()) lines(r, cx, cy, innerLength.floatValue(), innerThickness.floatValue(), innerGap.floatValue() + extra,
                withOpacity(base, innerOpacity.floatValue()));
        if (dot.on()) {
            int size = Math.max(1, Math.round(dotSize.floatValue()));
            float x1 = (float) Math.floor(cx + 0.5f - size / 2f), y1 = (float) Math.floor(cy + 0.5f - size / 2f);
            box(r, x1, y1, x1 + size, y1 + size, withOpacity(base, dotOpacity.floatValue()));
        }
    }

    /**
     * Four lines around the middle pixel (cx, cy)…(cx+1, cy+1). With an even thickness the extra pixel
     * goes to the top/left, like in most shooters.
     */
    private void lines(Renderer r, float cx, float cy, float len, float thick, float gap, int col) {
        int t = Math.max(1, Math.round(thick)), l = Math.max(1, Math.round(len)), g = Math.round(gap);
        float a = cx + 0.5f - t / 2f, b = a + t;          // across (x for vertical lines)
        float c = cy + 0.5f - t / 2f, d = c + t;          // across (y for horizontal lines)
        a = (float) Math.floor(a); b = a + t; c = (float) Math.floor(c); d = c + t;
        box(r, cx + 1 + g, c, cx + 1 + g + l, d, col);    // right
        box(r, cx - g - l, c, cx - g, d, col);            // left
        box(r, a, cy + 1 + g, b, cy + 1 + g + l, col);    // bottom
        if (!tShape.on()) box(r, a, cy - g - l, b, cy - g, col); // top
    }

    /** A filled rectangle with the optional outline around it. */
    private void box(Renderer r, float x1, float y1, float x2, float y2, int col) {
        if ((col >>> 24) == 0) return;
        if (outline.on()) {
            float o = Math.max(1, Math.round(outlineThickness.floatValue()));
            int oc = outlineColor.argb();
            int a = Math.round((oc >>> 24) * ((col >>> 24) / 255f));
            r.fill(x1 - o, y1 - o, x2 + o, y2 + o, (a << 24) | (oc & 0xFFFFFF));
        }
        r.fill(x1, y1, x2, y2, col);
    }

    /** Small bar below the crosshair while the attack is recharging (like the vanilla indicator). */
    private void drawAttack(Renderer r, float cx, float cy, float scale) {
        float charge = platform().attackCharge();
        if (charge >= 1f) return;
        float w = Math.round(16 * scale), h = Math.max(2, Math.round(2 * scale));
        float below = Math.max(innerGap.floatValue() + innerLength.floatValue(), 4 * scale);
        float x = cx + 0.5f - w / 2, y = cy + 1 + below + Math.round(4 * scale);
        r.fill(x, y, x + w, y + h, 0x80000000);
        r.fill(x, y, x + Math.round(w * charge), y + h, 0xFFFFFFFF);
    }

    private static int withOpacity(int argb, float percent) {
        int a = Math.round((argb >>> 24) * percent / 100f);
        return (a << 24) | (argb & 0xFFFFFF);
    }
}
