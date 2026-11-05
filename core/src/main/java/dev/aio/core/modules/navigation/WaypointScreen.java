package dev.aio.core.modules.navigation;

import dev.aio.core.i18n.Lang;

import dev.aio.core.AioCore;
import dev.aio.core.platform.Keys;
import dev.aio.core.platform.Renderer;
import dev.aio.core.platform.Vec3;
import dev.aio.core.ui.Theme;
import dev.aio.core.ui.UiScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/** Verwaltung der Wegpunkte dieser Welt: umbenennen, Farbe, ein-/ausblenden, löschen, neu setzen. */
public final class WaypointScreen implements UiScreen {
    private record Hit(float x1, float y1, float x2, float y2, IntConsumer action) {}

    private static final int ROW = 22;

    private final AioCore core;
    private final WaypointsModule module;
    private final List<Hit> hits = new ArrayList<>();
    private float scroll;
    private Waypoint editing;
    private String buffer = "";
    private boolean bufferAll;
    private Waypoint confirmDelete;

    public WaypointScreen(AioCore core, WaypointsModule module) {
        this.core = core;
        this.module = module;
    }

    @Override public String title() { return Lang.tr("aio.ui.waypoint.title", "Waypoints"); }

    @Override public void render(Renderer r, int mx, int my, float delta) {
        hits.clear();
        int w = r.screenWidth(), h = r.screenHeight();
        r.fill(0, 0, w, h, Theme.OVERLAY);
        int pw = Math.min(w - 20, 420), ph = Math.min(h - 20, 260);
        int px = (w - pw) / 2, py = (h - ph) / 2;
        r.fill(px, py, px + pw, py + ph, Theme.PANEL);
        Theme.accentBar(r, core, px, py, px + pw, py + 2);
        r.text(Lang.tr("aio.ui.waypoint.waypoints", "Waypoints"), px + 10, py + 9, Theme.TEXT, true);
        button(r, mx, my, px + pw - 110, py + 6, px + pw - 8, py + 20, Lang.tr("aio.ui.waypoint.add_here", "+ Add here"), true,
                b -> core.platform().openScreen(new WaypointEditScreen(core, module, null)));

        List<Waypoint> list = module.inThisWorld();
        Vec3 pos = core.platform().playerPos();
        String dim = core.platform().dimension();
        int top = py + 28, bottom = py + ph - 8;
        r.enableScissor(px, top, px + pw, bottom);
        float y = top - scroll;
        if (list.isEmpty()) r.text(Lang.tr("aio.ui.waypoint.no_waypoints_yet_press_b", "No waypoints yet. Press B or \"+ Add here\"."), px + 10, top + 6, Theme.TEXT_DIM, false);
        for (Waypoint wp : list) {
            if (y + ROW >= top && y <= bottom) drawRow(r, mx, my, wp, px + 8, y, pw - 16, pos, dim, top, bottom);
            y += ROW + 2;
        }
        r.disableScissor();
        float max = Math.max(0, list.size() * (ROW + 2) - (bottom - top));
        scroll = Math.max(0, Math.min(max, scroll));
    }

    private void drawRow(Renderer r, int mx, int my, Waypoint wp, float x, float y, float w, Vec3 pos, String dim, int top, int bottom) {
        r.fill(x, y, x + w, y + ROW, Theme.CARD);
        int c = wp.color() | 0xFF000000;
        r.fill(x + 4, y + 5, x + 16, y + 17, c);
        hitClipped(x + 4, y + 5, x + 16, y + 17, top, bottom, b -> module.replace(wp, wp.withColor(nextColor(wp.color()))));

        float nameX = x + 22;
        if (editing == wp) {
            r.fill(nameX - 2, y + 4, nameX + 120, y + 18, 0xFF2A2A34);
            if (bufferAll) dev.aio.core.ui.TextEdit.drawSelection(r, nameX, y + 7, buffer);
            boolean caret = !bufferAll && (System.currentTimeMillis() / 500) % 2 == 0;
            r.text(buffer + (caret ? "_" : ""), nameX, y + 7, Theme.TEXT, false);
        } else {
            r.text(r.trim(wp.name(), 118), nameX, y + 7, wp.visible() ? Theme.TEXT : Theme.TEXT_MUTED, false);
            hitClipped(nameX - 2, y + 4, nameX + 120, y + 18, top, bottom, b -> {
                editing = wp;
                buffer = wp.name();
                bufferAll = false;
            });
        }
        String coords = wp.x() + " " + wp.y() + " " + wp.z() + (wp.dimension().equals(dim) ? "" : "  (" + dimName(wp.dimension()) + ")");
        boolean ch = mx >= x + 146 && mx <= x + 146 + r.textWidth(coords) + 8 && my >= y + 4 && my <= y + 18;
        r.text(coords + (ch ? "  ✎" : ""), x + 150, y + 7, ch ? Theme.TEXT : Theme.TEXT_DIM, false);
        hitClipped(x + 146, y + 4, x + 146 + r.textWidth(coords) + 14, y + 18, top, bottom,
                b -> core.platform().openScreen(new WaypointEditScreen(core, module, wp)));
        if (wp.dimension().equals(dim)) {
            String d = WaypointsModule.formatDistance(wp.distanceTo(pos.x(), pos.y(), pos.z()));
            r.text(d, x + w - 92 - r.textWidth(d), y + 7, Theme.TEXT_MUTED, false);
        }
        button(r, mx, my, x + w - 86, y + 4, x + w - 46, y + 18, wp.visible() ? Lang.tr("aio.ui.waypoint.off", "Off") : Lang.tr("aio.ui.waypoint.on", "On"), false,
                b -> module.replace(wp, wp.withVisible(!wp.visible())), top, bottom);
        boolean confirm = confirmDelete == wp;
        button(r, mx, my, x + w - 42, y + 4, x + w - 4, y + 18, confirm ? Lang.tr("aio.ui.waypoint.sure", "Sure?") : "✖", false, b -> {
            if (confirmDelete == wp) {
                module.remove(wp);
                confirmDelete = null;
            } else {
                confirmDelete = wp;
            }
        }, top, bottom);
    }

    private static String dimName(String dim) {
        return switch (dim) {
            case "overworld" -> Lang.tr("aio.ui.waypoint.overworld", "Overworld");
            case "the_nether" -> Lang.tr("aio.ui.waypoint.nether", "Nether");
            case "the_end" -> Lang.tr("aio.ui.waypoint.end", "End");
            default -> dim;
        };
    }

    private static int nextColor(int color) {
        int[] p = WaypointsModule.PALETTE;
        for (int i = 0; i < p.length; i++) if (p[i] == (color & 0xFFFFFF)) return p[(i + 1) % p.length];
        return p[0];
    }

    private void button(Renderer r, int mx, int my, float x1, float y1, float x2, float y2, String label, boolean accent, IntConsumer action) {
        button(r, mx, my, x1, y1, x2, y2, label, accent, action, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    private void button(Renderer r, int mx, int my, float x1, float y1, float x2, float y2, String label, boolean accent,
                        IntConsumer action, int top, int bottom) {
        boolean hover = mx >= x1 && mx <= x2 && my >= y1 && my <= y2;
        r.fill(x1, y1, x2, y2, accent ? Theme.accent(core) : hover ? 0xFF33333E : 0xFF24242C);
        r.centeredText(label, (x1 + x2) / 2, y1 + (y2 - y1 - 8) / 2, Theme.TEXT, false);
        hitClipped(x1, y1, x2, y2, top, bottom, action);
    }

    private void hitClipped(float x1, float y1, float x2, float y2, int top, int bottom, IntConsumer action) {
        float cy1 = Math.max(y1, top), cy2 = Math.min(y2, bottom);
        if (cy2 > cy1) hits.add(new Hit(x1, cy1, x2, cy2, action));
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        Waypoint wasEditing = editing;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (mx >= h.x1 && mx <= h.x2 && my >= h.y1 && my <= h.y2) {
                if (wasEditing != null) commit();
                h.action.accept(button);
                return true;
            }
        }
        commit();
        confirmDelete = null;
        return false;
    }

    private void commit() {
        if (editing != null) {
            String name = buffer.isBlank() ? editing.name() : buffer.trim();
            module.replace(editing, editing.withName(name));
            editing = null;
        }
    }

    @Override public boolean mouseScrolled(double x, double y, double amount) {
        scroll = (float) Math.max(0, scroll - amount * 20);
        return true;
    }

    @Override public boolean keyPressed(String key) {
        if (editing == null) return false;
        if (Keys.ENTER.equals(key)) commit();
        else if (Keys.ESCAPE.equals(key)) editing = null;
        else {
            var res = dev.aio.core.ui.TextEdit.key(core.platform(), buffer, bufferAll, key);
            buffer = res.text().length() > 32 ? res.text().substring(0, 32) : res.text();
            bufferAll = res.allSelected();
        }
        return true;
    }

    @Override public boolean charTyped(String text) {
        if (editing == null || text.isEmpty() || text.codePointAt(0) < 32) return false;
        buffer = dev.aio.core.ui.TextEdit.type(buffer, bufferAll, text);
        if (buffer.length() > 32) buffer = buffer.substring(0, 32);
        bufferAll = false;
        return true;
    }

    @Override public void onClose() { commit(); }
}
