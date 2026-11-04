package dev.aio.core.modules.performance;

import dev.aio.core.AioCore;
import dev.aio.core.platform.Keys;
import dev.aio.core.platform.Renderer;
import dev.aio.core.ui.Theme;
import dev.aio.core.ui.UiScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Liste aller Partikel mit Schaltern und Suche; oben die häufig abgeschalteten. */
public final class ParticleScreen implements UiScreen {
    private static final int ROW = 14;

    private final AioCore core;
    private final ParticleLimitModule module;
    private final List<String> all;
    private String query = "";
    private float scroll;
    private int contentHeight;
    private float listTop, listBottom;

    private record Hit(float x1, float y1, float x2, float y2, Runnable action) {
        boolean in(double x, double y) { return x >= x1 && x <= x2 && y >= y1 && y <= y2; }
    }
    private final List<Hit> hits = new ArrayList<>();

    public ParticleScreen(AioCore core, ParticleLimitModule module) {
        this.core = core;
        this.module = module;
        List<String> ids = new ArrayList<>(core.platform().particleTypes());
        ids.sort(String::compareTo);
        this.all = ids;
    }

    @Override public String title() { return "Partikel"; }

    @Override public boolean returnsToPrevious() { return true; }

    static String name(String id) {
        String n = ParticleLimitModule.IMPORTANT.get(id);
        if (n != null) return n;
        String path = id.substring(id.indexOf(':') + 1);
        StringBuilder sb = new StringBuilder();
        for (String w : path.split("_")) {
            if (w.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return sb.toString();
    }

    private boolean matches(String id) {
        if (query.isEmpty()) return true;
        String q = query.toLowerCase(Locale.ROOT);
        return id.toLowerCase(Locale.ROOT).contains(q) || name(id).toLowerCase(Locale.ROOT).contains(q);
    }

    @Override public void render(Renderer r, int mx, int my, float delta) {
        hits.clear();
        int w = r.screenWidth(), h = r.screenHeight();
        r.fill(0, 0, w, h, Theme.OVERLAY);
        float pw = Math.min(w - 16, 320), px = (w - pw) / 2f, py = 8, ph = h - 16;
        r.fill(px, py, px + pw, py + ph, Theme.PANEL);
        Theme.accentBar(r, core, px, py, px + pw, py + 2);
        r.text("Partikel", px + 8, py + 8, Theme.TEXT, true);
        String count = module.isActive() ? "" : "  (Modul ist aus)";
        r.text(count, px + 8 + r.textWidth("Partikel"), py + 8, Theme.WARNING, false);

        float sx = px + 8, sy = py + 22, sw = pw - 16;
        r.fill(sx, sy, sx + sw, sy + 14, 0xF0202028);
        r.outline(sx, sy, sx + sw, sy + 14, 1, Theme.accent(core));
        boolean caret = (System.currentTimeMillis() / 500) % 2 == 0;
        String shown = query.isEmpty() ? "Suchen… (einfach tippen)" : query + (caret ? "_" : "");
        r.text(r.trim(shown, sw - 8), sx + 4, sy + 3, query.isEmpty() ? Theme.TEXT_MUTED : Theme.TEXT, false);

        float by = sy + 18;
        float bx = px + 8;
        bx += button(r, bx, by, "Alle an", mx, my, () -> setAll(false)) + 3;
        button(r, bx, by, "Alle aus", mx, my, () -> setAll(true));

        listTop = by + 18;
        listBottom = py + ph - 6;
        r.enableScissor((int) px, (int) listTop, (int) (px + pw), (int) listBottom);
        float y = listTop - scroll;
        List<String> important = new ArrayList<>();
        for (String id : ParticleLimitModule.IMPORTANT.keySet()) if (matches(id) && (id.startsWith("aio:") || all.contains(id))) important.add(id);
        if (!important.isEmpty()) {
            y = header(r, "Wichtig", px + 8, y);
            for (String id : important) y = row(r, id, px + 8, pw - 16, y, mx, my);
        }
        List<String> rest = new ArrayList<>();
        for (String id : all) if (!ParticleLimitModule.IMPORTANT.containsKey(id) && matches(id)) rest.add(id);
        if (!rest.isEmpty()) {
            y = header(r, "Alle Partikel", px + 8, y + 4);
            for (String id : rest) y = row(r, id, px + 8, pw - 16, y, mx, my);
        }
        if (important.isEmpty() && rest.isEmpty()) r.text("Nichts gefunden.", px + 8, listTop + 4, Theme.TEXT_DIM, false);
        r.disableScissor();
        contentHeight = (int) (y + scroll - listTop);
    }

    private void setAll(boolean off) {
        List<String> ids = new ArrayList<>(ParticleLimitModule.IMPORTANT.keySet());
        ids.addAll(all);
        for (String id : ids) if (matches(id)) module.setDisabled(id, off);
    }

    private float header(Renderer r, String text, float x, float y) {
        if (y + ROW > listTop && y < listBottom) r.text(text, x, y + 3, Theme.accent(core), true);
        return y + ROW;
    }

    private float row(Renderer r, String id, float x, float w, float y, int mx, int my) {
        if (y + ROW > listTop && y < listBottom) {
            boolean on = !module.isDisabled(id);
            boolean hover = mx >= x && mx <= x + w && my >= y && my < y + ROW && my >= listTop && my <= listBottom;
            if (hover) r.fill(x - 2, y, x + w + 2, y + ROW, 0x20FFFFFF);
            r.text(r.trim(name(id), w - 60), x, y + 3, on ? Theme.TEXT : Theme.TEXT_MUTED, false);
            r.text(id.startsWith("aio:") ? "" : id, x + w - 40 - r.textWidth(id), y + 3, 0xFF505060, false);
            // Schalter
            float tx = x + w - 26, ty = y + 3;
            r.fill(tx, ty, tx + 22, ty + 8, on ? Theme.accent(core) : Theme.TRACK);
            float knob = on ? tx + 14 : tx;
            r.fill(knob, ty, knob + 8, ty + 8, 0xFFFFFFFF);
            if (my >= listTop && my <= listBottom) hits.add(new Hit(x, y, x + w, y + ROW, () -> module.setDisabled(id, on)));
        }
        return y + ROW;
    }

    private float button(Renderer r, float x, float y, String label, int mx, int my, Runnable action) {
        float bw = r.textWidth(label) + 10;
        boolean hover = mx >= x && mx <= x + bw && my >= y && my <= y + 13;
        r.fill(x, y, x + bw, y + 13, hover ? Theme.CARD_HOVER : Theme.CARD);
        r.text(label, x + 5, y + 3, Theme.TEXT, false);
        hits.add(new Hit(x, y, x + bw, y + 13, action));
        return bw;
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        for (Hit h : hits) {
            if (h.in(mx, my) && button == 0) {
                h.action.run();
                return true;
            }
        }
        return false;
    }

    @Override public boolean mouseScrolled(double x, double y, double amount) {
        float max = Math.max(0, contentHeight - (listBottom - listTop));
        scroll = Math.max(0, Math.min(max, scroll - (float) amount * ROW * 2));
        return true;
    }

    @Override public boolean keyPressed(String key) {
        if (Keys.BACKSPACE.equals(key)) {
            if (!query.isEmpty()) query = query.substring(0, query.length() - 1);
            scroll = 0;
            return true;
        }
        return false;
    }

    @Override public boolean charTyped(String text) {
        if (text.isEmpty() || text.codePointAt(0) < 32) return false;
        if (query.length() < 40) query += text;
        scroll = 0;
        return true;
    }
}
