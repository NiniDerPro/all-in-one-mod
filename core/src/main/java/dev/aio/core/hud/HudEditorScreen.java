package dev.aio.core.hud;

import dev.aio.core.i18n.Lang;

import dev.aio.core.AioCore;
import dev.aio.core.modules.chat.ChatModule;
import dev.aio.core.platform.Keys;
import dev.aio.core.platform.Renderer;
import dev.aio.core.ui.Theme;
import dev.aio.core.ui.UiScreen;

import java.util.ArrayList;
import java.util.List;

/**
 * HUD-Editor wie in einem Grafikprogramm.
 *
 * <ul>
 *   <li>Linksklick wählt ein Element aus, Ziehen verschiebt es (rastet an Rändern und anderen
 *       Elementen ein, Shift = frei).</li>
 *   <li>Das ausgewählte Element hat einen Rahmen mit acht Griffen: an Kanten und Ecken ziehen
 *       ändert Breite und Höhe unabhängig, Shift an einer Ecke behält das Seitenverhältnis.</li>
 *   <li>Rechtsklick öffnet die Einstellungen, R/Entf oder der Knopf oben setzt die Auswahl zurück.</li>
 *   <li>Teile eines Moduls (Uhrzeit/Datum, Rüstungsteile) sind einzeln verschiebbar. Ziehen auf
 *       freier Fläche zieht einen Auswahlrahmen: alles darin wird gemeinsam verschoben. Strg+Klick
 *       fügt hinzu/entfernt, Doppelklick wählt alle Teile desselben Moduls.</li>
 *   <li>Der Chat lässt sich ebenso auswählen, oben und rechts in der Größe ziehen und zurücksetzen.
 *       Seine Position legt Minecraft fest (unten links).</li>
 * </ul>
 */
public final class HudEditorScreen implements UiScreen {
    private static final float SNAP_THRESHOLD = 5f;
    /** Fangbereich der Griffe in Pixeln (innen und außen um die Kante). */
    private static final float GRIP = 4f;
    private static final float MIN_PX = 6f;

    /** Kanten eines Griffs als Bitmaske. */
    private static final int LEFT = 1, RIGHT = 2, TOP = 4, BOTTOM = 8;

    private final AioCore core;

    /** Ausgewähltes Ziel: ein {@link HudTarget} oder {@link #CHAT}. */
    private Object selected;
    /** Mehrfachauswahl (Auswahlrahmen, Strg+Klick, Doppelklick). Leer oder ≥ 2 Einträge. */
    private final List<HudTarget> group = new ArrayList<>();
    private float[] groupStart;
    private float marqueeX, marqueeY;
    private long lastClickTime;
    private Object lastClickTarget;
    private static final Object CHAT = new Object();

    private enum Mode { NONE, MOVE, RESIZE, MARQUEE, GROUP_MOVE }
    private Mode mode = Mode.NONE;
    private int edges;
    /** Startzustand beim Ziehen: Maus und Rechteck. */
    private float startMx, startMy, startX, startY, startW, startH;
    private Snapper.Result lastSnap;
    private int lastMouseX, lastMouseY;
    private Renderer lastRenderer;

    public HudEditorScreen(AioCore core) {
        this.core = core;
        core.hud().setEditorOpen(true);
    }

    @Override public String title() { return Lang.tr("aio.ui.hud_editor.title", "HUD Editor"); }

    private List<HudElement> visibleElements() {
        List<HudElement> list = new ArrayList<>();
        for (HudElement el : core.hud().elements()) if (el.isActive() && el.editable()) list.add(el);
        return list;
    }

    /** Alles, was einzeln verschoben werden kann: Elemente bzw. ihre Teile, wenn getrennt. */
    private List<HudTarget> targets() {
        List<HudTarget> list = new ArrayList<>();
        for (HudElement el : visibleElements()) {
            if (el.separated()) list.addAll(el.visibleParts(true));
            else list.add(el);
        }
        return list;
    }

    private boolean valid(Object t) {
        if (t == CHAT) return chat() != null;
        return t instanceof HudTarget ht && targets().contains(ht);
    }

    // =====================================================================
    // Ziele (Element oder Chat) als Rechteck
    // =====================================================================

    private ChatModule chat() {
        ChatModule c = core.module(ChatModule.class);
        return c != null && c.isActive() ? c : null;
    }

    /** x, y, Breite, Höhe des Ziels oder null. */
    private float[] bounds(Renderer r, Object target) {
        if (target instanceof HudTarget el) {
            return new float[]{el.screenX(r), el.screenY(r), el.scaledWidth(r), el.scaledHeight(r)};
        }
        if (target == CHAT) {
            ChatModule c = chat();
            if (c == null) return null;
            float bottom = r.screenHeight() - 40;
            return new float[]{2, bottom - c.boxHeight(), c.boxWidth(), c.boxHeight()};
        }
        return null;
    }

    /** Welche Kanten am Ziel gezogen werden dürfen. */
    private static int allowedEdges(Object target) {
        return target == CHAT ? RIGHT | TOP : LEFT | RIGHT | TOP | BOTTOM;
    }

    private Object targetAt(Renderer r, double mx, double my) {
        if (r == null) return null;
        List<HudTarget> list = targets();
        for (int i = list.size() - 1; i >= 0; i--) {
            if (inside(bounds(r, list.get(i)), mx, my, 0)) return list.get(i);
        }
        float[] cb = bounds(r, CHAT);
        return inside(cb, mx, my, 0) ? CHAT : null;
    }

    private static boolean inside(float[] b, double mx, double my, float pad) {
        return b != null && mx >= b[0] - pad && mx <= b[0] + b[2] + pad && my >= b[1] - pad && my <= b[1] + b[3] + pad;
    }

    /** Kanten unter der Maus am ausgewählten Ziel (0 = keine). */
    private int edgesAt(Renderer r, double mx, double my) {
        float[] b = bounds(r, selected);
        if (b == null || !inside(b, mx, my, GRIP)) return 0;
        // Bei kleinen Elementen den Fangbereich verkleinern, damit die Mitte zum Verschieben bleibt
        float gx = Math.min(GRIP, b[2] / 3f), gy = Math.min(GRIP, b[3] / 3f);
        int e = 0;
        if (Math.abs(mx - b[0]) <= gx) e |= LEFT;
        else if (Math.abs(mx - (b[0] + b[2])) <= gx) e |= RIGHT;
        if (Math.abs(my - b[1]) <= gy) e |= TOP;
        else if (Math.abs(my - (b[1] + b[3])) <= gy) e |= BOTTOM;
        return e & allowedEdges(selected);
    }

    private String nameOf(Object target) {
        if (target instanceof HudTarget el) return el.name();
        return target == CHAT ? Lang.tr("aio.ui.hud_editor.chat", "Chat") : "";
    }

    // =====================================================================
    // Zeichnen
    // =====================================================================

    @Override public void render(Renderer r, int mouseX, int mouseY, float delta) {
        lastRenderer = r;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        if (selected != null && !valid(selected)) selected = null;
        group.removeIf(t -> !valid(t));
        if (group.size() < 2) group.clear();

        core.hud().placeNew(r);
        int w = r.screenWidth(), h = r.screenHeight();
        r.fill(0, 0, w, h, 0x30000000);
        r.fill(w / 2f - 0.5f, 0, w / 2f + 0.5f, h, 0x18FFFFFF);
        r.fill(0, h / 2f - 0.5f, w, h / 2f + 0.5f, 0x18FFFFFF);

        int accent = Theme.accent(core);
        Object hovered = mode != Mode.NONE ? selected : targetAt(r, mouseX, mouseY);

        // Chat-Fläche
        float[] cb = bounds(r, CHAT);
        if (cb != null) {
            chat().renderPreview(r, cb[0], cb[1] + cb[3]);
            if (selected != CHAT) frame(r, cb, hovered == CHAT ? accent : 0x60FFFFFF);
        }

        for (HudElement el : visibleElements()) {
            el.update();
            el.render(r, true);
        }
        for (HudTarget t : targets()) {
            if (t == selected) continue;
            boolean inGroup = group.contains(t);
            frame(r, bounds(r, t), inGroup ? accent : t == hovered ? accent : 0x60FFFFFF);
            if (inGroup) {
                float[] b = bounds(r, t);
                r.fill(b[0], b[1], b[0] + b[2], b[1] + b[3], (accent & 0x00FFFFFF) | 0x30000000);
            }
        }
        if (mode == Mode.MARQUEE) {
            float x1 = Math.min(marqueeX, mouseX), y1 = Math.min(marqueeY, mouseY);
            float x2 = Math.max(marqueeX, mouseX), y2 = Math.max(marqueeY, mouseY);
            r.fill(x1, y1, x2, y2, (accent & 0x00FFFFFF) | 0x28000000);
            r.outline(x1, y1, x2, y2, 1, accent);
        }

        // Auswahl mit Griffen zuletzt, damit sie über allem liegt
        float[] sb = bounds(r, selected);
        if (sb != null) {
            frame(r, sb, accent);
            int hot = mode == Mode.RESIZE ? edges : edgesAt(r, mouseX, mouseY);
            drawHandles(r, sb, allowedEdges(selected), hot, accent);
        }

        if ((mode == Mode.MOVE || mode == Mode.GROUP_MOVE) && lastSnap != null) {
            if (lastSnap.hasGuideX()) r.fill(lastSnap.guideX() - 0.5f, 0, lastSnap.guideX() + 0.5f, h, accent);
            if (lastSnap.hasGuideY()) r.fill(0, lastSnap.guideY() - 0.5f, w, lastSnap.guideY() + 0.5f, accent);
        }

        Object info = mode == Mode.MARQUEE || mode == Mode.GROUP_MOVE ? null : mode != Mode.NONE ? selected : hovered;
        if (info != null) tooltip(r, info, mouseX, mouseY);
        if (!group.isEmpty() && mode != Mode.MARQUEE) {
            String sel = group.size() + " selected";
            r.centeredText(sel, w / 2f, 26, accent, true);
        }

        String hint = Lang.tr("aio.ui.hud_editor.drag_move_edges_resize_drag", "Drag: move · Edges: resize · Drag on empty space: select several · Ctrl+click: add · Double click: all parts · Right click: settings · R: reset");
        if (r.textWidth(hint) > w - 8) hint = Lang.tr("aio.ui.hud_editor.drag_edges_resize_drag_empty", "Drag · Edges: resize · Drag empty: select · Ctrl+click · Double click · R");
        if (r.textWidth(hint) > w - 8) hint = Lang.tr("aio.ui.hud_editor.drag_edges_select_right_click", "Drag · Edges · Select · Right click · R");
        r.centeredText(hint, w / 2f, h - 14, 0xB0FFFFFF, true);

        drawButton(r, settingsButton(r), Lang.tr("aio.ui.hud_editor.settings", "Settings"), true, mouseX, mouseY);
        drawButton(r, resetButton(r), Lang.tr("aio.ui.hud_editor.reset", "Reset"), selected != null || !group.isEmpty(), mouseX, mouseY);
    }

    private static void frame(Renderer r, float[] b, int color) {
        if (b == null) return;
        r.outline(b[0] - 1, b[1] - 1, b[0] + b[2] + 1, b[1] + b[3] + 1, 1, color);
    }

    private static void drawHandles(Renderer r, float[] b, int allowed, int hot, int accent) {
        float x1 = b[0] - 1, y1 = b[1] - 1, x2 = b[0] + b[2] + 1, y2 = b[1] + b[3] + 1;
        float mx = (x1 + x2) / 2f, my = (y1 + y2) / 2f;
        int[] e = {LEFT | TOP, TOP, RIGHT | TOP, RIGHT, RIGHT | BOTTOM, BOTTOM, LEFT | BOTTOM, LEFT};
        float[][] p = {{x1, y1}, {mx, y1}, {x2, y1}, {x2, my}, {x2, y2}, {mx, y2}, {x1, y2}, {x1, my}};
        for (int i = 0; i < e.length; i++) {
            if ((e[i] & allowed) != e[i]) continue;
            float s = 2.5f;
            int fill = e[i] == hot ? accent : 0xFFFFFFFF;
            r.fill(p[i][0] - s - 0.5f, p[i][1] - s - 0.5f, p[i][0] + s + 0.5f, p[i][1] + s + 0.5f, 0xFF000000);
            r.fill(p[i][0] - s, p[i][1] - s, p[i][0] + s, p[i][1] + s, fill);
        }
    }

    private void tooltip(Renderer r, Object target, int mx, int my) {
        String label;
        if (target instanceof HudTarget el) {
            label = el.name() + "  W " + Math.round(el.scaleX() * 100) + "% · H " + Math.round(el.scaleY() * 100) + "%";
        } else {
            float[] b = bounds(r, target);
            if (b == null) return;
            label = nameOf(target) + "  " + Math.round(b[2]) + " × " + Math.round(b[3]);
        }
        int w = r.screenWidth(), h = r.screenHeight();
        float lx = Math.min(mx + 10, w - r.textWidth(label) - 4);
        float ly = Math.min(my + 10, h - 12);
        r.fill(lx - 3, ly - 3, lx + r.textWidth(label) + 3, ly + r.fontHeight() + 1, 0xC0101014);
        r.text(label, lx, ly, 0xFFFFFFFF, false);
    }

    private float[] settingsButton(Renderer r) {
        float bw = r.textWidth(Lang.tr("aio.ui.hud_editor.settings", "Settings")) + 16;
        float x = r.screenWidth() / 2f - bw - 3;
        return new float[]{x, 6, x + bw, 22};
    }

    private float[] resetButton(Renderer r) {
        float bw = r.textWidth(Lang.tr("aio.ui.hud_editor.reset", "Reset")) + 16;
        float x = r.screenWidth() / 2f + 3;
        return new float[]{x, 6, x + bw, 22};
    }

    private void drawButton(Renderer r, float[] b, String label, boolean enabled, int mx, int my) {
        boolean hover = enabled && mx >= b[0] && mx <= b[2] && my >= b[1] && my <= b[3];
        r.fill(b[0], b[1], b[2], b[3], hover ? Theme.accent(core) : 0xD0181820);
        r.centeredText(label, (b[0] + b[2]) / 2f, b[1] + 4, enabled ? 0xFFFFFFFF : 0x70FFFFFF, false);
    }

    private static boolean on(float[] b, double mx, double my) {
        return mx >= b[0] && mx <= b[2] && my >= b[1] && my <= b[3];
    }

    // =====================================================================
    // Eingabe
    // =====================================================================

    @Override public boolean mouseClicked(double mx, double my, int button) {
        Renderer r = lastRenderer;
        if (r == null) return false;
        if (button == 0 && on(settingsButton(r), mx, my)) {
            core.openClickGui(null);
            return true;
        }
        if (button == 0 && on(resetButton(r), mx, my)) {
            resetSelected();
            return true;
        }

        if (button == 1) {
            Object t = targetAt(r, mx, my);
            if (t instanceof HudTarget el) core.openClickGui(el.owner());
            else if (t == CHAT) core.openClickGui(chat());
            return t != null;
        }
        if (button != 0) return false;

        // Erst die Griffe der Auswahl (liegen teils außerhalb des Elements)
        int e = group.isEmpty() ? edgesAt(r, mx, my) : 0;
        if (e != 0) {
            begin(r, Mode.RESIZE, e, mx, my);
            return true;
        }
        Object t = targetAt(r, mx, my);
        long now = System.currentTimeMillis();
        boolean doubleClick = t != null && t == lastClickTarget && now - lastClickTime < 350;
        lastClickTime = now;
        lastClickTarget = t;

        if (t == null) {
            // Freie Fläche: Auswahlrahmen aufziehen
            selected = null;
            if (!ctrl()) group.clear();
            mode = Mode.MARQUEE;
            marqueeX = (float) mx;
            marqueeY = (float) my;
            return true;
        }
        if (t instanceof HudTarget ht) {
            if (doubleClick && ht.owner().separated()) {
                // Doppelklick: alle Teile desselben Moduls
                group.clear();
                group.addAll(ht.owner().visibleParts(true));
                if (group.size() < 2) group.clear();
                selected = group.isEmpty() ? ht : null;
                if (!group.isEmpty()) beginGroup(r, mx, my);
                return true;
            }
            if (ctrl()) {
                // Strg+Klick: zur Gruppe hinzufügen/entfernen
                if (group.isEmpty() && selected instanceof HudTarget cur && cur != ht) group.add(cur);
                if (group.contains(ht)) group.remove(ht);
                else group.add(ht);
                selected = null;
                if (group.size() == 1) {
                    selected = group.get(0);
                    group.clear();
                }
                return true;
            }
            if (group.contains(ht)) {
                beginGroup(r, mx, my);
                return true;
            }
        }
        group.clear();
        selected = t;
        // Der Chat sitzt fest unten links, dort nur auswählen
        begin(r, t == CHAT ? Mode.NONE : Mode.MOVE, 0, mx, my);
        return true;
    }

    private boolean ctrl() {
        return core.platform().isKeyDown(Keys.LEFT_CONTROL) || core.platform().isKeyDown(Keys.RIGHT_CONTROL);
    }

    private void beginGroup(Renderer r, double mx, double my) {
        mode = Mode.GROUP_MOVE;
        lastSnap = null;
        startMx = (float) mx;
        startMy = (float) my;
        groupStart = new float[group.size() * 2];
        for (int i = 0; i < group.size(); i++) {
            groupStart[i * 2] = group.get(i).screenX(r);
            groupStart[i * 2 + 1] = group.get(i).screenY(r);
        }
    }

    /** Verschiebt die Gruppe gemeinsam; der umgebende Rahmen rastet wie ein einzelnes Element ein. */
    private void moveGroup(Renderer r, float ddx, float ddy, boolean free) {
        if (groupStart == null || groupStart.length != group.size() * 2) return;
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (int i = 0; i < group.size(); i++) {
            HudTarget t = group.get(i);
            minX = Math.min(minX, groupStart[i * 2]);
            minY = Math.min(minY, groupStart[i * 2 + 1]);
            maxX = Math.max(maxX, groupStart[i * 2] + t.scaledWidth(r));
            maxY = Math.max(maxY, groupStart[i * 2 + 1] + t.scaledHeight(r));
        }
        float gw = maxX - minX, gh = maxY - minY;
        float nx = minX + ddx, ny = minY + ddy;
        if (free) {
            lastSnap = new Snapper.Result(Math.max(0, Math.min(r.screenWidth() - gw, nx)),
                    Math.max(0, Math.min(r.screenHeight() - gh, ny)), Float.NaN, Float.NaN);
        } else {
            List<Snapper.Rect> others = new ArrayList<>();
            for (HudTarget o : targets()) {
                if (!group.contains(o)) others.add(new Snapper.Rect(o.screenX(r), o.screenY(r), o.scaledWidth(r), o.scaledHeight(r)));
            }
            lastSnap = Snapper.snap(new Snapper.Rect(nx, ny, gw, gh), others, r.screenWidth(), r.screenHeight(), SNAP_THRESHOLD);
        }
        float dx = lastSnap.x() - minX, dy = lastSnap.y() - minY;
        for (int i = 0; i < group.size(); i++) group.get(i).setScreenPos(r, groupStart[i * 2] + dx, groupStart[i * 2 + 1] + dy);
    }

    private void finishMarquee(Renderer r, double mx, double my) {
        float x1 = Math.min(marqueeX, (float) mx), y1 = Math.min(marqueeY, (float) my);
        float x2 = Math.max(marqueeX, (float) mx), y2 = Math.max(marqueeY, (float) my);
        if (x2 - x1 < 2 && y2 - y1 < 2) return; // nur geklickt
        for (HudTarget t : targets()) {
            float[] b = bounds(r, t);
            boolean hit = b[0] < x2 && b[0] + b[2] > x1 && b[1] < y2 && b[1] + b[3] > y1;
            if (hit && !group.contains(t)) group.add(t);
        }
        if (group.size() == 1) {
            selected = group.get(0);
            group.clear();
        }
    }

    private void begin(Renderer r, Mode m, int e, double mx, double my) {
        float[] b = bounds(r, selected);
        mode = b == null ? Mode.NONE : m;
        edges = e;
        lastSnap = null;
        if (b == null) return;
        startMx = (float) mx;
        startMy = (float) my;
        startX = b[0];
        startY = b[1];
        startW = b[2];
        startH = b[3];
    }

    @Override public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        Renderer r = lastRenderer;
        if (r == null || button != 0 || mode == Mode.NONE) return false;
        if (mode == Mode.MARQUEE) return true;
        float ddx = (float) mx - startMx, ddy = (float) my - startMy;
        boolean shift = core.platform().isKeyDown(Keys.LEFT_SHIFT) || core.platform().isKeyDown(Keys.RIGHT_SHIFT);
        if (mode == Mode.GROUP_MOVE) {
            moveGroup(r, ddx, ddy, shift);
            return true;
        }
        if (selected == null) return false;
        if (mode == Mode.MOVE) move(r, startX + ddx, startY + ddy, shift);
        else resize(r, ddx, ddy, shift);
        return true;
    }

    private void move(Renderer r, float nx, float ny, boolean free) {
        if (!(selected instanceof HudTarget el)) return;
        float w = el.scaledWidth(r), h = el.scaledHeight(r);
        if (free) {
            lastSnap = new Snapper.Result(
                    Math.max(0, Math.min(r.screenWidth() - w, nx)),
                    Math.max(0, Math.min(r.screenHeight() - h, ny)), Float.NaN, Float.NaN);
        } else {
            List<Snapper.Rect> others = new ArrayList<>();
            for (HudTarget o : targets()) {
                if (o != el) others.add(new Snapper.Rect(o.screenX(r), o.screenY(r), o.scaledWidth(r), o.scaledHeight(r)));
            }
            lastSnap = Snapper.snap(new Snapper.Rect(nx, ny, w, h), others, r.screenWidth(), r.screenHeight(), SNAP_THRESHOLD);
        }
        el.setScreenPos(r, lastSnap.x(), lastSnap.y());
    }

    private void resize(Renderer r, float ddx, float ddy, boolean keepRatio) {
        float sw = r.screenWidth(), sh = r.screenHeight();
        float x1 = startX, y1 = startY, x2 = startX + startW, y2 = startY + startH;
        if ((edges & LEFT) != 0) x1 = Math.max(0, Math.min(x2 - MIN_PX, x1 + ddx));
        if ((edges & RIGHT) != 0) x2 = Math.min(sw, Math.max(x1 + MIN_PX, x2 + ddx));
        if ((edges & TOP) != 0) y1 = Math.max(0, Math.min(y2 - MIN_PX, y1 + ddy));
        if ((edges & BOTTOM) != 0) y2 = Math.min(sh, Math.max(y1 + MIN_PX, y2 + ddy));

        boolean corner = (edges & (LEFT | RIGHT)) != 0 && (edges & (TOP | BOTTOM)) != 0;
        if (keepRatio && corner && startW > 0 && startH > 0) {
            // Den größeren relativen Zug nehmen und die andere Seite nachziehen
            float f = Math.max((x2 - x1) / startW, (y2 - y1) / startH);
            float nw = startW * f, nh = startH * f;
            if ((edges & LEFT) != 0) x1 = x2 - nw; else x2 = x1 + nw;
            if ((edges & TOP) != 0) y1 = y2 - nh; else y2 = y1 + nh;
        }

        if (selected instanceof HudTarget el) {
            el.setScreenBounds(r, x1, y1, x2 - x1, y2 - y1);
            // Begrenzung der Skalierung: die gegenüberliegende Kante bleibt stehen
            float w = el.scaledWidth(r), h = el.scaledHeight(r);
            float nx = (edges & LEFT) != 0 ? x2 - w : x1;
            float ny = (edges & TOP) != 0 ? y2 - h : y1;
            el.setScreenPos(r, nx, ny);
        } else if (selected == CHAT && chat() != null) {
            chat().resizeTo(x2 - x1, y2 - y1);
        }
    }

    @Override public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0 && mode != Mode.NONE) {
            if (mode == Mode.MARQUEE && lastRenderer != null) finishMarquee(lastRenderer, mx, my);
            mode = Mode.NONE;
            lastSnap = null;
            return true;
        }
        return false;
    }

    /** Das Mausrad ändert im Editor bewusst nichts mehr (Größe nur über die Griffe). */
    @Override public boolean mouseScrolled(double mx, double my, double amount) { return false; }

    @Override public boolean keyPressed(String key) {
        if ("key.keyboard.a".equals(key) && ctrl()) {
            // Strg+A: alles auswählen
            selected = null;
            group.clear();
            group.addAll(targets());
            if (group.size() < 2) group.clear();
            return true;
        }
        if (Keys.ESCAPE.equals(key) && (!group.isEmpty() || selected != null)) {
            group.clear();
            selected = null;
            return true;
        }
        if (Keys.R.equals(key) || Keys.DELETE.equals(key) || Keys.BACKSPACE.equals(key)) {
            if (selected == null && group.isEmpty()) selected = targetAt(lastRenderer, lastMouseX, lastMouseY);
            return resetSelected();
        }
        return false;
    }

    private boolean resetSelected() {
        if (!group.isEmpty()) {
            for (HudTarget t : group) t.resetLayout();
            return true;
        }
        if (selected instanceof HudTarget el) {
            el.resetLayout();
            return true;
        }
        if (selected == CHAT && chat() != null) {
            chat().resetBox();
            return true;
        }
        return false;
    }

    @Override public void onOpen() { core.hud().setEditorOpen(true); }

    /** ESC führt dorthin zurück, woher man kam (z. B. ins Menü). */
    @Override public boolean returnsToPrevious() { return true; }

    @Override public void onClose() {
        core.hud().setEditorOpen(false);
        mode = Mode.NONE;
    }
}
