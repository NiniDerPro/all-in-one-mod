package dev.aio.core.modules.hud;

import dev.aio.core.hud.ColorMode;
import dev.aio.core.hud.HudElement;
import dev.aio.core.platform.ItemRef;
import dev.aio.core.platform.LookTarget;
import dev.aio.core.platform.Renderer;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.EnumSetting;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Blick-Info im Stil von Jade/WAILA: Tooltip oben in der Mitte mit dem Block oder Wesen
 * unter dem Fadenkreuz. Zeigt Icon, Name, Werkzeug, Wachstum, Leben, Rüstung und die Mod.
 *
 * <p>Eigene Implementierung, kein Code aus Jade übernommen.
 */
public final class LookAtModule extends HudElement {
    public enum Style implements EnumSetting.Named {
        CLASSIC("Klassisch"), COLORFUL("Farbmodus");

        private final String displayName;

        Style(String displayName) { this.displayName = displayName; }

        @Override public String displayName() { return displayName; }
    }

    /** Eine Textzeile; {@code hearts} markiert die Herzzeile, die gesondert gezeichnet wird. */
    private record Line(String text, int color, boolean hearts) {}

    // Farben des klassischen Tooltips
    static final int BG = 0xF0100010;
    static final int BORDER_TOP = 0x505000FF;
    static final int BORDER_BOTTOM = 0x5028007F;
    static final int INFO = 0xFFAAAAAA;
    static final int MOD = 0xFF5555FF;
    static final int GOOD = 0xFF55FF55;
    static final int BAD = 0xFFFF5555;

    private static final int PAD = 6;
    private static final int ICON = 16;
    private static final int LINE_H = 10;
    private static final int MAX_HEARTS = 10;
    private static final String HEART = "❤";

    private final EnumSetting<Style> style;
    private final BooleanSetting showIcon;
    private final BooleanSetting showModName;
    private final BooleanSetting showTool;
    private final BooleanSetting showGrowth;
    private final BooleanSetting showHealth;
    private final BooleanSetting showArmor;
    private final BooleanSetting showCoords;
    private final BooleanSetting hideInMenus;

    private LookTarget target;
    private String name = "";
    private List<Line> lines = List.of();
    private String key = "";
    private int cachedW = -1;
    private int heartsW;

    public LookAtModule() {
        super("lookat", "Blick-Info", "Zeigt Block oder Wesen unter dem Fadenkreuz (wie Jade)", 0.5, 0.0);
        style = add(new EnumSetting<>("style", "Stil", Style.CLASSIC));
        showIcon = add(new BooleanSetting("showIcon", "Icon", true));
        showModName = add(new BooleanSetting("showModName", "Mod-Name", true));
        showTool = add(new BooleanSetting("showTool", "Werkzeug", true));
        showGrowth = add(new BooleanSetting("showGrowth", "Wachstum", true));
        showHealth = add(new BooleanSetting("showHealth", "Leben", true));
        showArmor = add(new BooleanSetting("showArmor", "Rüstung", true));
        showCoords = add(new BooleanSetting("showCoords", "Koordinaten", false));
        hideInMenus = add(new BooleanSetting("hideInMenus", "In Menüs ausblenden", true));
    }

    @Override protected int defaultBackgroundColor() { return BG; }

    @Override public boolean enabledByDefault() { return true; }

    @Override protected ColorMode defaultColorMode() { return ColorMode.STATIC; }

    @Override protected void onActivate() { update(); }

    // ---- Daten ----

    @Override public void update() {
        LookTarget t = platform().inWorld() ? platform().lookTarget() : null;
        setTarget(t);
    }

    /** Baut Name und Zeilen neu, aber nur wenn sich der sichtbare Zustand geändert hat. */
    void setTarget(LookTarget t) {
        target = t;
        if (t == null) {
            if (!key.isEmpty()) {
                key = "";
                name = "";
                lines = List.of();
                cachedW = -1;
            }
            return;
        }
        List<Line> next = new ArrayList<>(5);
        if (t.kind() == LookTarget.Kind.BLOCK) {
            if (showTool.on() && t.toolType() != null) {
                // ✔ passendes Werkzeug in der Hand, ✘ ohne Werkzeug kein Drop, sonst neutral
                if (t.correctTool() == null) next.add(new Line(t.toolType(), INFO, false));
                else if (t.correctTool()) next.add(new Line("✔ " + t.toolType(), GOOD, false));
                else next.add(new Line("✘ " + t.toolType(), BAD, false));
            }
            if (showGrowth.on() && !Float.isNaN(t.growth())) {
                next.add(new Line(t.growth() >= 1f ? "Wachstum: ausgewachsen"
                        : "Wachstum: " + Math.round(t.growth() * 100) + "%", INFO, false));
            }
        } else {
            if (showHealth.on() && !Float.isNaN(t.health()) && t.maxHealth() > 0) {
                boolean asHearts = t.maxHealth() <= MAX_HEARTS * 2;
                String text = asHearts ? "" : HEART + " " + fmt(t.health()) + " / " + fmt(t.maxHealth());
                next.add(new Line(text, BAD, asHearts));
            }
            if (showArmor.on() && t.armor() > 0) next.add(new Line("Rüstung: " + t.armor(), INFO, false));
        }
        if (showCoords.on()) next.add(new Line(t.x() + ", " + t.y() + ", " + t.z(), INFO, false));
        if (showModName.on() && t.source() != null && !t.source().isEmpty()) {
            next.add(new Line("§o" + t.source(), MOD, false));
        }

        StringBuilder k = new StringBuilder(t.name());
        for (Line l : next) k.append('|').append(l.text);
        if (!Float.isNaN(t.health())) k.append('|').append(t.health()).append('/').append(t.maxHealth());
        k.append('|').append(t.icon() == null || t.icon().isEmpty() ? "" : t.icon().name());
        String newKey = k.toString();
        if (!newKey.equals(key)) {
            key = newKey;
            cachedW = -1;
        }
        name = t.name();
        lines = next;
    }

    private static String fmt(float v) {
        return v == Math.floor(v) ? String.valueOf((int) v) : String.format(Locale.ROOT, "%.1f", v);
    }

    // ---- Layout ----

    @Override public boolean hasContent() {
        return target != null && !(hideInMenus.on() && platform().isScreenOpen());
    }

    private boolean previewing(boolean preview) { return preview && target == null; }

    private boolean hasIcon(boolean preview) {
        if (!showIcon.on()) return false;
        if (previewing(preview)) return true;
        return target != null && target.icon() != null && !target.icon().isEmpty();
    }

    private String shownName(boolean preview) { return previewing(preview) ? "Grasblock" : name; }

    private List<Line> shownLines(boolean preview) {
        if (!previewing(preview)) return lines;
        List<Line> demo = new ArrayList<>();
        if (showTool.on()) demo.add(new Line("✔ Schaufel", GOOD, false));
        if (showModName.on()) demo.add(new Line("§oMinecraft", MOD, false));
        return demo;
    }

    private int textLeft(boolean preview) { return PAD + (hasIcon(preview) ? ICON + 4 : 0); }

    private int heartsWidth(Renderer r) {
        if (target == null || Float.isNaN(target.maxHealth())) return 0;
        int n = (int) Math.ceil(target.maxHealth() / 2f);
        return n * (r.textWidth(HEART) + 1);
    }

    @Override public int width(Renderer r) {
        // Breite für Vorschau und echtes Ziel kann sich unterscheiden; gecacht wird nur das echte Ziel.
        boolean preview = target == null;
        if (!preview && cachedW >= 0) return cachedW;
        int w = r.textWidth(shownName(preview));
        heartsW = heartsWidth(r);
        for (Line l : shownLines(preview)) w = Math.max(w, l.hearts ? heartsW : r.textWidth(l.text));
        int total = textLeft(preview) + w + PAD;
        if (!preview) cachedW = total;
        return total;
    }

    @Override public int height(Renderer r) {
        boolean preview = target == null;
        int textH = (1 + shownLines(preview).size()) * LINE_H - 1;
        int content = Math.max(hasIcon(preview) ? ICON : 0, textH);
        return content + PAD * 2 - 2;
    }

    @Override protected boolean drawsDefaultBackground() { return false; }

    // ---- Zeichnen ----

    @Override protected void renderContent(Renderer r, boolean preview) {
        int w = width(r), h = height(r);
        drawTooltipBox(r, w, h);

        int left = textLeft(preview);
        List<Line> ls = shownLines(preview);
        int textH = (1 + ls.size()) * LINE_H - 1;
        float top = (h - textH) / 2f + 1;

        if (hasIcon(preview)) {
            float iy = (h - ICON) / 2f;
            if (previewing(preview)) r.fill(PAD, iy, PAD + ICON, iy + ICON, 0x6055AA44);
            else r.item(target.icon(), PAD, iy, false);
        }

        drawText(r, shownName(preview), left, top);
        float y = top + LINE_H;
        for (Line l : ls) {
            if (l.hearts) drawHearts(r, left, y);
            else r.text(l.text, left, y, l.color, shadow());
            y += LINE_H;
        }
    }

    private void drawTooltipBox(Renderer r, int w, int h) {
        if (!background.on()) return;
        int bg = backgroundColor.argb();
        // Hintergrund mit abgeschrägten Ecken wie beim Vanilla-Tooltip
        r.fill(1, 0, w - 1, h, bg);
        r.fill(0, 1, 1, h - 1, bg);
        r.fill(w - 1, 1, w, h - 1, bg);

        int top, bottom;
        if (style.get() == Style.COLORFUL) {
            top = dynamicColor(0) | 0xFF000000;
            bottom = dynamicColor(60) | 0xFF000000;
        } else {
            top = BORDER_TOP;
            bottom = BORDER_BOTTOM;
        }
        r.fill(1, 1, w - 1, 2, top);
        r.fill(1, h - 2, w - 1, h - 1, bottom);
        r.fillGradient(1, 2, 2, h - 2, top, bottom);
        r.fillGradient(w - 2, 2, w - 1, h - 2, top, bottom);
    }

    private void drawHearts(Renderer r, float x, float y) {
        float hp = Math.max(0, target.health());
        int n = (int) Math.ceil(target.maxHealth() / 2f);
        int step = r.textWidth(HEART) + 1;
        for (int i = 0; i < n; i++) {
            float filled = hp - i * 2;
            int color = filled >= 2 ? BAD : filled > 0 ? 0xFFB03030 : 0xFF3A3A3A;
            r.text(HEART, x + i * step, y, color, shadow());
        }
    }

    // Für Tests
    String targetName() { return name; }

    List<String> lineTexts() {
        List<String> out = new ArrayList<>();
        for (Line l : lines) out.add(l.hearts ? "<hearts>" : l.text);
        return out;
    }

    /** Für Tests: ob das Item-Icon gezeigt würde. */
    boolean iconShown() { return hasIcon(false); }

    ItemRef icon() { return target == null ? null : target.icon(); }
}
