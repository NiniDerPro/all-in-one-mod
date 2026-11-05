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
 * Gemeinsame Basis aller HUD-Elemente: Position, Skalierung, Farbe, Hintergrund, Schatten, Sichtbarkeit.
 *
 * <p>Die Position wird als Anteil 0..1 des freien Platzes gespeichert
 * ({@code x * (Bildschirmbreite - Elementbreite)}). Ein Element am rechten Rand bleibt dadurch
 * auch bei anderer Auflösung oder GUI-Skalierung am rechten Rand.
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
    /** Ob das Element schon einen Platz bekommen hat (beim ersten Einschalten automatisch). */
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
        // Breite und Höhe getrennt, damit man im HUD-Editor an jeder Kante/Ecke frei ziehen kann.
        // Die ID "scale" bleibt für alte Configs (dort war es die gemeinsame Größe).
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

    // ---- Einzeln verschiebbare Teile ----

    private final List<HudPart> parts = new ArrayList<>();
    private BooleanSetting separate;

    /**
     * Fügt einen einzeln verschiebbaren Teil hinzu (im Konstruktor der Unterklasse aufrufen).
     * Beim ersten Teil entsteht die Einstellung "Move parts separately".
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

    /** Ob die Teile gerade einzeln stehen (sonst ein zusammenhängender Block). */
    public boolean separated() { return separate != null && separate.on() && !parts.isEmpty(); }

    /** Teile, die gerade angezeigt werden (im Editor alle). */
    public List<HudPart> visibleParts(boolean preview) {
        List<HudPart> out = new ArrayList<>();
        for (HudPart p : parts) if (partEnabled(p) && (preview || partHasContent(p))) out.add(p);
        return out;
    }

    /** Ob ein Teil per Einstellung überhaupt gezeigt wird (z. B. "Show date" aus). */
    protected boolean partEnabled(HudPart part) { return true; }

    protected boolean partHasContent(HudPart part) { return true; }

    protected int partWidth(Renderer r, HudPart part) { return width(r); }

    protected int partHeight(Renderer r, HudPart part) { return height(r); }

    /** Lage des Teils im zusammenhängenden Element (unskaliert), Startplatz beim Trennen. */
    protected float[] partOffset(Renderer r, HudPart part) { return new float[]{0, 0}; }

    /** Zeichnet einen Teil im lokalen Koordinatensystem (0,0 = obere linke Ecke des Teils). */
    protected void renderPart(Renderer r, HudPart part, boolean preview) {}

    /** Für Elemente, die frei über den Bildschirm zeichnen (Marker, Namensschilder): Box-Einstellungen ausblenden. */
    protected final void hideBoxSettings() {
        for (var s : new Setting<?>[]{posX, posY, scale, scaleY, colorMode, textColor, secondColor,
                colorSpeed, background, backgroundColor, shadow}) {
            s.hidden();
        }
    }

    /** Standard-Farbmodus. Wird im Konstruktor abgefragt, darf also nur eine Konstante liefern. */
    protected ColorMode defaultColorMode() { return ColorMode.CLEAN; }

    /** Standard-Textfarbe (ARGB). Wie oben: nur eine Konstante liefern. */
    protected int defaultTextColor() { return 0xFFFFFFFF; }

    /** Ob der Hintergrund standardmäßig an ist. Wie oben: nur eine Konstante liefern. */
    protected boolean defaultBackground() { return true; }

    /** Standard-Hintergrundfarbe (ARGB). Wie oben: nur eine Konstante liefern. */
    protected int defaultBackgroundColor() { return DEFAULT_BACKGROUND; }

    /** Neuer Standard-Hintergrund: dunkles Graphit, leicht durchsichtig. */
    public static final int DEFAULT_BACKGROUND = 0x8C0E1015;

    /** Einmalig beim Wechsel aufs neue Design: alte Standardwerte (Regenbogen, Grau) übernehmen den neuen Stil. */
    public void migrateStyle() {
        if (colorMode.get() == ColorMode.CHROMA && defaultColorMode() != ColorMode.CHROMA) colorMode.reset();
        if (backgroundColor.argb() == 0x6F000000) backgroundColor.reset();
    }

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
     * Setzt das Element auf ein Rechteck in Bildschirmpixeln (HUD-Editor). Breite und Höhe werden
     * auf den erlaubten Bereich begrenzt.
     */
    public void setScreenBounds(Renderer r, float x, float y, float w, float h) {
        int bw = Math.max(1, width(r)), bh = Math.max(1, height(r));
        scale.set(clampScale(w / bw));
        scaleY.set(clampScale(h / bh));
        setScreenPos(r, x, y);
    }

    /** Position und Größe auf die Standardwerte zurücksetzen. */
    public void resetLayout() {
        scale.reset();
        scaleY.reset();
        posX.reset();
        posY.reset();
        for (HudPart p : parts) p.resetLayout();
    }

    static double clampScale(double v) { return Math.max(MIN_SCALE, Math.min(MAX_SCALE, v)); }

    /** Abstand zum Bildschirmrand, damit nichts direkt am Rand klebt. */
    public static final float EDGE = 3;

    /** Bildschirm-Position aus dem gespeicherten Anteil (0 = linker Rand, 1 = rechter Rand). */
    static float toScreen(double fraction, float screen, float size) {
        float free = screen - size - 2 * EDGE;
        return free <= 0 ? Math.max(0, (screen - size) / 2f) : EDGE + (float) (fraction * free);
    }

    static double toFraction(float pos, float screen, float size) {
        float free = screen - size - 2 * EDGE;
        return free <= 0 ? 0 : clamp01((pos - EDGE) / free);
    }

    public float screenX(Renderer r) { return toScreen(posX.get(), r.screenWidth(), scaledWidth(r)); }

    public float screenY(Renderer r) { return toScreen(posY.get(), r.screenHeight(), scaledHeight(r)); }

    public void setScreenPos(Renderer r, float x, float y) {
        posX.set(toFraction(x, r.screenWidth(), scaledWidth(r)));
        posY.set(toFraction(y, r.screenHeight(), scaledHeight(r)));
    }

    public NumberSetting scaleSetting() { return scale; }

    public boolean isPlaced() { return placed.on(); }

    public void markPlaced() { placed.set(true); }

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
