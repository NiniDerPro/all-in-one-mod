package dev.aio.core.modules.render;

import dev.aio.core.AioCore;
import dev.aio.core.i18n.Lang;
import dev.aio.core.modules.render.CrosshairModule.Style;
import dev.aio.core.platform.Renderer;
import dev.aio.core.ui.Draw;
import dev.aio.core.ui.Theme;
import dev.aio.core.ui.UiScreen;
import dev.aio.core.ui.Window;

import java.util.List;

/** Crosshair presets as cards with a live preview; a click takes the preset over. */
public final class CrosshairScreen implements UiScreen {
    private static final int CARD_W = 76, CARD_H = 62, GAP = 6;

    private final AioCore core;
    private final CrosshairModule module;
    private float scroll;
    private int px, py, pw, ph, top, cols;

    public CrosshairScreen(AioCore core, CrosshairModule module) {
        this.core = core;
        this.module = module;
    }

    @Override public String title() { return Lang.tr("aio.ui.crosshair.title", "Crosshair presets"); }

    @Override public void render(Renderer r, int mx, int my, float delta) {
        int w = r.screenWidth(), h = r.screenHeight();
        r.fill(0, 0, w, h, Theme.OVERLAY);
        pw = Math.min(w - 20, 5 * (CARD_W + GAP) + GAP + 120);
        ph = Math.min(h - 20, 2 * (CARD_H + GAP) + 50);
        px = (w - pw) / 2;
        py = (h - ph) / 2;
        Window.frame(r, px, py, px + pw, py + ph);
        Window.title(r, title(), px + 10, py + 9);
        String hint = Lang.tr("aio.ui.crosshair.hint", "Click a preset; fine-tune it in the module settings");
        r.text(r.trim(hint, pw - 140), px + 10, py + ph - 14, Theme.TEXT_MUTED, false);

        // current crosshair on the right
        float cx1 = px + pw - 112, cy1 = py + 28;
        Draw.round(r, cx1, cy1, cx1 + 102, cy1 + 92, 3, Theme.CARD);
        r.centeredText(Lang.tr("aio.ui.crosshair.current", "Current"), cx1 + 51, cy1 + 6, Theme.TEXT_DIM, false);
        preview(r, cx1 + 51, cy1 + 52, module.current(), true);

        // presets
        List<Style> presets = CrosshairModule.PRESETS;
        top = py + 28;
        int areaW = pw - 132;
        cols = Math.max(1, (areaW + GAP) / (CARD_W + GAP));
        int bottom = py + ph - 20;
        r.enableScissor(px, top, px + pw - 120, bottom);
        for (int i = 0; i < presets.size(); i++) {
            Style st = presets.get(i);
            float x = px + 10 + (i % cols) * (CARD_W + GAP);
            float y = top + (i / cols) * (CARD_H + GAP) - scroll;
            if (y + CARD_H < top || y > bottom) continue;
            boolean active = module.matches(st);
            boolean hover = mx >= x && mx <= x + CARD_W && my >= y && my <= y + CARD_H && my >= top && my <= bottom;
            Draw.round(r, x, y, x + CARD_W, y + CARD_H, 3, hover ? Theme.CARD_HOVER : Theme.CARD);
            if (active) Draw.roundOutline(r, x, y, x + CARD_W, y + CARD_H, Theme.accent(core));
            preview(r, x + CARD_W / 2f, y + 26, st, false);
            r.centeredText(r.trim(name(st), CARD_W - 6), x + CARD_W / 2f, y + CARD_H - 13, active ? Theme.accent(core) : Theme.TEXT, false);
        }
        r.disableScissor();
        int rows = (presets.size() + cols - 1) / cols;
        float max = Math.max(0, rows * (CARD_H + GAP) - (bottom - top));
        scroll = Math.max(0, Math.min(max, scroll));
    }

    /** Draws a crosshair at real pixel size around (cx, cy) in GUI coordinates, on a dark field. */
    private void preview(Renderer r, float cx, float cy, Style st, boolean big) {
        float half = big ? 34 : 20;
        r.fill(cx - half, cy - half * 0.8f, cx + half, cy + half * 0.8f, 0xFF2B3340);
        float scale = (float) Math.max(1, core.platform().guiScale());
        r.push();
        r.scale(1f / scale);
        CrosshairModule.draw(r, Math.round(cx * scale), Math.round(cy * scale), st, 0, module.rainbowSpeed());
        r.pop();
    }

    private static String name(Style st) {
        return switch (st.name()) {
            case "Classic" -> Lang.tr("aio.ui.crosshair.preset.classic", "Classic");
            case "Dot" -> Lang.tr("aio.ui.crosshair.preset.dot", "Dot");
            case "Tactical" -> Lang.tr("aio.ui.crosshair.preset.tactical", "Tactical");
            case "Small cross" -> Lang.tr("aio.ui.crosshair.preset.small_cross", "Small cross");
            case "Plus" -> Lang.tr("aio.ui.crosshair.preset.plus", "Plus");
            case "T shape" -> Lang.tr("aio.ui.crosshair.preset.t_shape", "T shape");
            case "Sniper" -> Lang.tr("aio.ui.crosshair.preset.sniper", "Sniper");
            case "Bold" -> Lang.tr("aio.ui.crosshair.preset.bold", "Bold");
            case "Outer only" -> Lang.tr("aio.ui.crosshair.preset.outer_only", "Outer only");
            case "Rainbow" -> Lang.tr("aio.ui.crosshair.preset.rainbow", "Rainbow");
            default -> st.name();
        };
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        List<Style> presets = CrosshairModule.PRESETS;
        int bottom = py + ph - 20;
        if (my < top || my > bottom) return false;
        for (int i = 0; i < presets.size(); i++) {
            float x = px + 10 + (i % cols) * (CARD_W + GAP);
            float y = top + (i / cols) * (CARD_H + GAP) - scroll;
            if (mx >= x && mx <= x + CARD_W && my >= y && my <= y + CARD_H) {
                module.apply(presets.get(i));
                return true;
            }
        }
        return false;
    }

    @Override public boolean mouseScrolled(double x, double y, double amount) {
        scroll = (float) Math.max(0, scroll - amount * 20);
        return true;
    }

    @Override public boolean returnsToPrevious() { return true; }
}
