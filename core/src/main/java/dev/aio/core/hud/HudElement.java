package dev.aio.core.hud;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.platform.Renderer;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.ColorSetting;
import dev.aio.core.setting.EnumSetting;
import dev.aio.core.setting.NumberSetting;

/**
 * Gemeinsame Basis aller HUD-Elemente: Position, Skalierung, Farbe, Hintergrund, Schatten, Sichtbarkeit.
 *
 * <p>Die Position wird als Anteil 0..1 des freien Platzes gespeichert
 * ({@code x * (Bildschirmbreite - Elementbreite)}). Ein Element am rechten Rand bleibt dadurch
 * auch bei anderer Auflösung oder GUI-Skalierung am rechten Rand.
 */
public abstract class HudElement extends Module {
    public static final int PADDING = 4;

    protected final NumberSetting posX;
    protected final NumberSetting posY;
    protected final NumberSetting scale;
    protected final EnumSetting<ColorMode> colorMode;
    protected final ColorSetting textColor;
    protected final ColorSetting secondColor;
    protected final NumberSetting colorSpeed;
    protected final BooleanSetting background;
    protected final ColorSetting backgroundColor;
    protected final BooleanSetting shadow;

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
        scale = add(new NumberSetting("scale", "Größe", 1.0, 0.5, 3.0, 0.05, "x"));
        colorMode = add(new EnumSetting<>("colorMode", "Farbmodus", defaultColorMode()));
        textColor = add(new ColorSetting("textColor", "Textfarbe", 0xFFFFFFFF));
        textColor.visibleWhen(() -> colorMode.get() != ColorMode.CHROMA);
        secondColor = add(new ColorSetting("secondColor", "Zweite Farbe", 0xFF4FC3FF));
        secondColor.visibleWhen(() -> colorMode.get() == ColorMode.GRADIENT);
        colorSpeed = add(new NumberSetting("colorSpeed", "Farbtempo", 0.4, 0.05, 2, 0.05, "x"));
        colorSpeed.visibleWhen(() -> colorMode.get() != ColorMode.STATIC);
        background = add(new BooleanSetting("background", "Hintergrund", true));
        backgroundColor = add(new ColorSetting("backgroundColor", "Hintergrundfarbe", defaultBackgroundColor()));
        backgroundColor.visibleWhen(background::on);
        shadow = add(new BooleanSetting("shadow", "Textschatten", true));
    }

    /** Standard-Farbmodus. Wird im Konstruktor abgefragt, darf also nur eine Konstante liefern. */
    protected ColorMode defaultColorMode() { return ColorMode.CHROMA; }

    /** Standard-Hintergrundfarbe (ARGB). Wie oben: nur eine Konstante liefern. */
    protected int defaultBackgroundColor() { return 0x6F000000; }

    // ---- Inhalt (von Unterklassen) ----

    /** Breite ohne Skalierung, inklusive Innenabstand. */
    public abstract int width(Renderer r);

    /** Höhe ohne Skalierung, inklusive Innenabstand. */
    public abstract int height(Renderer r);

    /**
     * Zeichnet den Inhalt im lokalen Koordinatensystem (0,0 = obere linke Ecke, unskaliert).
     *
     * @param preview true im HUD-Editor: Platzhalter zeigen, wenn es nichts anzuzeigen gibt
     */
    protected abstract void renderContent(Renderer r, boolean preview);

    /** Zeichnet frei über den ganzen Bildschirm (z. B. Wegpunkt-Marker). Standard: nichts. */
    public void renderOverlay(Renderer r, float partialTick) {}

    /** Ob das Element im HUD-Editor verschoben werden kann. */
    public boolean editable() { return true; }

    /** Wird pro Tick aufgerufen, solange das Element aktiv ist. Ideal zum Aktualisieren von Caches. */
    public void update() {}

    /** Ob das Element im Spiel gerade etwas zu zeigen hat (z. B. Trank-Effekte nur mit Effekten). */
    public boolean hasContent() { return true; }

    /** Ob die Standard-Hintergrundbox gezeichnet werden soll. Keystrokes zeichnen z. B. eigene. */
    protected boolean drawsDefaultBackground() { return true; }

    // ---- Zeichnen ----

    public final void render(Renderer r, boolean preview) {
        if (!preview && !hasContent()) return;
        float s = scale.floatValue();
        float x = screenX(r);
        float y = screenY(r);
        r.push();
        r.translate(x, y);
        r.scale(s);
        if (background.on() && drawsDefaultBackground()) r.fill(0, 0, width(r), height(r), backgroundColor.argb());
        renderContent(r, preview);
        r.pop();
    }

    // ---- Position (Bildschirmpixel, skaliert) ----

    public float scaledWidth(Renderer r) { return width(r) * scale.floatValue(); }

    public float scaledHeight(Renderer r) { return height(r) * scale.floatValue(); }

    public float screenX(Renderer r) {
        return (float) Math.max(0, posX.get() * Math.max(0, r.screenWidth() - scaledWidth(r)));
    }

    public float screenY(Renderer r) {
        return (float) Math.max(0, posY.get() * Math.max(0, r.screenHeight() - scaledHeight(r)));
    }

    public void setScreenPos(Renderer r, float x, float y) {
        float freeW = r.screenWidth() - scaledWidth(r);
        float freeH = r.screenHeight() - scaledHeight(r);
        posX.set(freeW <= 0 ? 0 : clamp01(x / freeW));
        posY.set(freeH <= 0 ? 0 : clamp01(y / freeH));
    }

    public NumberSetting scaleSetting() { return scale; }

    protected int textColor() { return textColor.argb(); }

    /** Zeichnet Text im eingestellten Farbmodus (fest, Regenbogen, Verlauf). */
    protected void drawText(Renderer r, String text, float x, float y) {
        ColoredText.draw(r, text, x, y, colorMode.get(), textColor.argb(), secondColor.argb(),
                colorSpeed.floatValue(), shadow.on(), (float) (posX.get() * 300 + posY.get() * 120));
    }

    /** Dynamische Farbe für Flächen im eingestellten Farbmodus. */
    protected int dynamicColor(float offset) {
        return ColoredText.color(colorMode.get(), textColor.argb(), secondColor.argb(), colorSpeed.floatValue(),
                (float) (posX.get() * 300 + posY.get() * 120) + offset);
    }

    public ColorMode colorMode() { return colorMode.get(); }

    protected boolean shadow() { return shadow.on(); }

    private static double clamp01(double v) { return Math.max(0, Math.min(1, v)); }
}
