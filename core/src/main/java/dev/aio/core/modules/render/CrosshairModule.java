package dev.aio.core.modules.render;

import dev.aio.core.hud.HudElement;
import dev.aio.core.i18n.Lang;
import dev.aio.core.module.Category;
import dev.aio.core.platform.Perspective;
import dev.aio.core.platform.Renderer;
import dev.aio.core.platform.Vec3;
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

        outline.group("outline");
        outlineColor.group("outline");
        outlineThickness.group("outline");
        dot.group("dot");
        dotSize.group("dot");
        dotOpacity.group("dot");
        inner.group("inner");
        innerLength.group("inner");
        innerThickness.group("inner");
        innerGap.group("inner");
        innerOpacity.group("inner");
        outer.group("outer");
        outerLength.group("outer");
        outerThickness.group("outer");
        outerGap.group("outer");
        outerOpacity.group("outer");
        tShape.group("more");
        movementSpread.group("more");
        spreadAmount.group("more");
        attackIndicator.group("more");
        thirdPerson.group("more");
    }

    @Override public String groupName(String id) {
        return switch (id) {
            case "outline" -> Lang.tr("aio.ui.crosshair.group.outline", "Outline");
            case "dot" -> Lang.tr("aio.ui.crosshair.group.dot", "Centre dot");
            case "inner" -> Lang.tr("aio.ui.crosshair.group.inner", "Inner lines");
            case "outer" -> Lang.tr("aio.ui.crosshair.group.outer", "Outer lines");
            case "more" -> Lang.tr("aio.ui.crosshair.group.more", "More");
            default -> id;
        };
    }

    @Override public boolean hasPreview() { return true; }

    @Override public float previewHeight() { return 70; }

    /** Live preview on a neutral grey tile pattern, like an empty world. */
    @Override public void drawPreview(Renderer r, float x1, float y1, float x2, float y2) {
        float mid = (y1 + y2) / 2, tile = 6;
        r.fill(x1, y1, x2, y2, 0xFF3A3A3F);
        for (float ty = y1; ty < y2; ty += tile) {
            for (float tx = x1; tx < x2; tx += tile) {
                if ((Math.round((tx - x1) / tile) + Math.round((ty - y1) / tile)) % 2 == 0) {
                    r.fill(tx, ty, Math.min(x2, tx + tile), Math.min(y2, ty + tile), 0xFF44444A);
                }
            }
        }
        float scale = (float) Math.max(1, platform().guiScale());
        r.push();
        r.scale(1f / scale);
        draw(r, Math.round((x1 + x2) / 2 * scale), Math.round(mid * scale), current(), 0, rainbowSpeed.floatValue());
        r.pop();
    }

    @Override public String previewButtonLabel() { return Lang.tr("aio.ui.crosshair.presets_button", "Presets"); }

    @Override public void previewButtonClicked() { core.platform().openScreen(new CrosshairScreen(core, this)); }

    /** Everything that makes up the look of a crosshair (sizes in screen pixels, opacity in percent). */
    public record Style(String name, int color, boolean rainbow, boolean outline, int outlineColor, float outlineThickness,
                        boolean dot, float dotSize, float dotOpacity,
                        boolean inner, float innerLength, float innerThickness, float innerGap, float innerOpacity,
                        boolean outer, float outerLength, float outerThickness, float outerGap, float outerOpacity,
                        boolean tShape) {}

    private static final int BLACK = 0xB4000000;

    /** Ready-made looks; clicked in the preset screen they replace the current settings. */
    public static final java.util.List<Style> PRESETS = java.util.List.of(
            new Style("Classic", 0xFFFFFFFF, false, true, BLACK, 1, false, 2, 100, true, 6, 2, 3, 100, false, 4, 2, 14, 50, false),
            new Style("Dot", 0xFFFFFFFF, false, true, BLACK, 1, true, 4, 100, false, 6, 2, 3, 100, false, 4, 2, 14, 50, false),
            new Style("Tactical", 0xFF00FFAA, false, true, BLACK, 1, true, 2, 100, true, 4, 2, 4, 100, true, 4, 2, 16, 60, false),
            new Style("Small cross", 0xFF00FF00, false, true, BLACK, 1, false, 2, 100, true, 3, 2, 2, 100, false, 4, 2, 14, 50, false),
            new Style("Plus", 0xFFFFFF55, false, true, BLACK, 1, false, 2, 100, true, 6, 2, 0, 100, false, 4, 2, 14, 50, false),
            new Style("T shape", 0xFFFF5555, false, true, BLACK, 1, true, 2, 100, true, 6, 2, 3, 100, false, 4, 2, 14, 50, true),
            new Style("Sniper", 0xFFFF5555, false, false, BLACK, 1, true, 2, 100, true, 14, 1, 4, 100, false, 4, 2, 14, 50, false),
            new Style("Bold", 0xFF55FFFF, false, true, BLACK, 2, false, 2, 100, true, 8, 4, 4, 100, false, 4, 2, 14, 50, false),
            new Style("Outer only", 0xFFFFFFFF, false, true, BLACK, 1, true, 2, 100, false, 6, 2, 3, 100, true, 6, 2, 8, 100, false),
            new Style("Rainbow", 0xFFFFFFFF, true, true, BLACK, 1, true, 2, 100, true, 6, 2, 3, 100, false, 4, 2, 14, 50, false));

    /** The current look from the settings. */
    public Style current() {
        return new Style("", color.argb(), rainbow.on(), outline.on(), outlineColor.argb(), outlineThickness.floatValue(),
                dot.on(), dotSize.floatValue(), dotOpacity.floatValue(),
                inner.on(), innerLength.floatValue(), innerThickness.floatValue(), innerGap.floatValue(), innerOpacity.floatValue(),
                outer.on(), outerLength.floatValue(), outerThickness.floatValue(), outerGap.floatValue(), outerOpacity.floatValue(),
                tShape.on());
    }

    /** Takes over a preset (name ignored). */
    public void apply(Style st) {
        color.set(st.color()); rainbow.set(st.rainbow()); outline.set(st.outline()); outlineColor.set(st.outlineColor());
        outlineThickness.set((double) st.outlineThickness());
        dot.set(st.dot()); dotSize.set((double) st.dotSize()); dotOpacity.set((double) st.dotOpacity());
        inner.set(st.inner()); innerLength.set((double) st.innerLength()); innerThickness.set((double) st.innerThickness());
        innerGap.set((double) st.innerGap()); innerOpacity.set((double) st.innerOpacity());
        outer.set(st.outer()); outerLength.set((double) st.outerLength()); outerThickness.set((double) st.outerThickness());
        outerGap.set((double) st.outerGap()); outerOpacity.set((double) st.outerOpacity());
        tShape.set(st.tShape());
    }

    /** Whether the settings match a preset (for the highlight in the preset screen). */
    public boolean matches(Style st) {
        Style c = current();
        return new Style("", st.color(), st.rainbow(), st.outline(), st.outlineColor(), st.outlineThickness(), st.dot(), st.dotSize(),
                st.dotOpacity(), st.inner(), st.innerLength(), st.innerThickness(), st.innerGap(), st.innerOpacity(), st.outer(),
                st.outerLength(), st.outerThickness(), st.outerGap(), st.outerOpacity(), st.tShape()).equals(c);
    }

    public float rainbowSpeed() { return rainbowSpeed.floatValue(); }

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

    /** Draws the current crosshair with the middle pixel at (cx, cy), in screen pixels. */
    public void draw(Renderer r, float cx, float cy) {
        draw(r, cx, cy, current(), Math.round(spread * spreadAmount.floatValue()), rainbowSpeed.floatValue());
    }

    /** Draws any look (also the previews), sizes in screen pixels; (cx, cy) is the screen middle. */
    public static void draw(Renderer r, float cx, float cy, Style st, float extraGap, float rainbowSpeed) {
        int base = st.rainbow() ? Colors.chroma(0, rainbowSpeed, 255) : st.color();
        java.util.List<float[]> boxes = new java.util.ArrayList<>(); // x1, y1, x2, y2, colour
        if (st.outer()) lines(boxes, st, cx, cy, st.outerLength(), st.outerThickness(), st.outerGap() + extraGap, withOpacity(base, st.outerOpacity()));
        if (st.inner()) lines(boxes, st, cx, cy, st.innerLength(), st.innerThickness(), st.innerGap() + extraGap, withOpacity(base, st.innerOpacity()));
        if (st.dot()) {
            int size = Math.max(1, Math.round(st.dotSize()));
            float x1 = square(cx, size), y1 = square(cy, size);
            boxes.add(new float[]{x1, y1, x1 + size, y1 + size, withOpacity(base, st.dotOpacity())});
        }
        // first every outline, then every fill: an outline never covers a neighbouring line
        if (st.outline()) {
            float o = Math.max(1, Math.round(st.outlineThickness()));
            int oc = st.outlineColor();
            for (float[] b : boxes) {
                int col = (int) b[4];
                if ((col >>> 24) == 0) continue;
                int a = Math.round((oc >>> 24) * ((col >>> 24) / 255f));
                r.fill(b[0] - o, b[1] - o, b[2] + o, b[3] + o, (a << 24) | (oc & 0xFFFFFF));
            }
        }
        for (float[] b : boxes) if ((((int) b[4]) >>> 24) != 0) r.fill(b[0], b[1], b[2], b[3], (int) b[4]);
    }

    /** Start of a centred run of {@code size} pixels around the middle {@code c} (on the pixel grid). */
    private static float square(float c, int size) {
        return (float) Math.floor(c - size / 2f);
    }

    /**
     * Four arms around a centre square of the line thickness, all the same length and the same distance
     * from it, so the cross is exactly symmetric. With no gap the centre square is filled too (a plus).
     */
    private static void lines(java.util.List<float[]> out, Style st, float cx, float cy, float len, float thick, float gap, int col) {
        int t = Math.max(1, Math.round(thick)), l = Math.max(1, Math.round(len)), g = Math.max(0, Math.round(gap));
        float a = square(cx, t), b = a + t;   // columns of the vertical arms
        float c = square(cy, t), d = c + t;   // rows of the horizontal arms
        out.add(new float[]{b + g, c, b + g + l, d, col});        // right
        out.add(new float[]{a - g - l, c, a - g, d, col});        // left
        out.add(new float[]{a, d + g, b, d + g + l, col});        // bottom
        if (!st.tShape()) out.add(new float[]{a, c - g - l, b, c - g, col}); // top
        if (g == 0) out.add(new float[]{a, c, b, d, col});        // closed middle
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
