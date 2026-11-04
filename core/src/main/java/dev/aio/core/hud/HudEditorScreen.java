package dev.aio.core.hud;

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

    /** Ausgewähltes Ziel: ein HUD-Element oder {@link #CHAT}. */
    private Object selected;
    private static final Object CHAT = new Object();

    private enum Mode { NONE, MOVE, RESIZE }
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

    @Override public String title() { return "HUD-Editor"; }

    private List<HudElement> visibleElements() {
        List<HudElement> list = new ArrayList<>();
        for (HudElement el : core.hud().elements()) if (el.isActive() && el.editable()) list.add(el);
        return list;
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
        if (target instanceof HudElement el) {
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
        List<HudElement> list = visibleElements();
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
        if (target instanceof HudElement el) return el.name();
        return target == CHAT ? "Chat" : "";
    }

    // =====================================================================
    // Zeichnen
    // =====================================================================

    @Override public void render(Renderer r, int mouseX, int mouseY, float delta) {
        lastRenderer = r;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        if (selected == CHAT && chat() == null) selected = null;
        if (selected instanceof HudElement el && !(el.isActive() && el.editable())) selected = null;

        int w = r.screenWidth(), h = r.screenHeight();
        r.fill(0, 0, w, h, 0x30000000);
        r.fill(w / 2f - 0.5f, 0, w / 2f + 0.5f, h, 0x18FFFFFF);
        r.fill(0, h / 2f - 0.5f, w, h / 2f + 0.5f, 0x18FFFFFF);

        int accent = Theme.accent(core);
        Object hovered = mode != Mode.NONE ? selected : targetAt(r, mouseX, mouseY);

        // Chat-Fläche
        float[] cb = bounds(r, CHAT);
        if (cb != null) {
            r.fill(cb[0], cb[1], cb[0] + cb[2], cb[1] + cb[3], 0x40000000);
            r.text("Chat", cb[0] + 4, cb[1] + 4, 0xC0FFFFFF, true);
            if (selected != CHAT) frame(r, cb, hovered == CHAT ? accent : 0x60FFFFFF);
        }

        for (HudElement el : visibleElements()) {
            el.update();
            el.render(r, true);
            if (el != selected) frame(r, bounds(r, el), el == hovered ? accent : 0x60FFFFFF);
        }

        // Auswahl mit Griffen zuletzt, damit sie über allem liegt
        float[] sb = bounds(r, selected);
        if (sb != null) {
            frame(r, sb, accent);
            int hot = mode == Mode.RESIZE ? edges : edgesAt(r, mouseX, mouseY);
            drawHandles(r, sb, allowedEdges(selected), hot, accent);
        }

        if (mode == Mode.MOVE && lastSnap != null) {
            if (lastSnap.hasGuideX()) r.fill(lastSnap.guideX() - 0.5f, 0, lastSnap.guideX() + 0.5f, h, accent);
            if (lastSnap.hasGuideY()) r.fill(0, lastSnap.guideY() - 0.5f, w, lastSnap.guideY() + 0.5f, accent);
        }

        Object info = mode != Mode.NONE ? selected : hovered;
        if (info != null) tooltip(r, info, mouseX, mouseY);

        String hint = "Linksklick: auswählen/ziehen · Rand/Ecke: Größe · Shift: frei/Seitenverhältnis · Rechtsklick: Einstellungen · R: zurücksetzen";
        if (r.textWidth(hint) > w - 8) hint = "Ziehen · Rand/Ecke · Shift · Rechtsklick · R";
        r.centeredText(hint, w / 2f, h - 14, 0xB0FFFFFF, true);

        drawButton(r, settingsButton(r), "Einstellungen", true, mouseX, mouseY);
        drawButton(r, resetButton(r), "Zurücksetzen", selected != null, mouseX, mouseY);
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
        if (target instanceof HudElement el) {
            label = el.name() + "  B " + Math.round(el.scaleX() * 100) + "% · H " + Math.round(el.scaleY() * 100) + "%";
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
        float bw = r.textWidth("Einstellungen") + 16;
        float x = r.screenWidth() / 2f - bw - 3;
        return new float[]{x, 6, x + bw, 22};
    }

    private float[] resetButton(Renderer r) {
        float bw = r.textWidth("Zurücksetzen") + 16;
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
            if (t instanceof HudElement el) core.openClickGui(el);
            else if (t == CHAT) core.openClickGui(chat());
            return t != null;
        }
        if (button != 0) return false;

        // Erst die Griffe der Auswahl (liegen teils außerhalb des Elements)
        int e = edgesAt(r, mx, my);
        if (e != 0) {
            begin(r, Mode.RESIZE, e, mx, my);
            return true;
        }
        Object t = targetAt(r, mx, my);
        selected = t;
        if (t == null) return false;
        // Der Chat sitzt fest unten links, dort nur auswählen
        begin(r, t == CHAT ? Mode.NONE : Mode.MOVE, 0, mx, my);
        return true;
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
        if (r == null || button != 0 || mode == Mode.NONE || selected == null) return false;
        float ddx = (float) mx - startMx, ddy = (float) my - startMy;
        boolean shift = core.platform().isKeyDown(Keys.LEFT_SHIFT) || core.platform().isKeyDown(Keys.RIGHT_SHIFT);
        if (mode == Mode.MOVE) move(r, startX + ddx, startY + ddy, shift);
        else resize(r, ddx, ddy, shift);
        return true;
    }

    private void move(Renderer r, float nx, float ny, boolean free) {
        if (!(selected instanceof HudElement el)) return;
        float w = el.scaledWidth(r), h = el.scaledHeight(r);
        if (free) {
            lastSnap = new Snapper.Result(
                    Math.max(0, Math.min(r.screenWidth() - w, nx)),
                    Math.max(0, Math.min(r.screenHeight() - h, ny)), Float.NaN, Float.NaN);
        } else {
            List<Snapper.Rect> others = new ArrayList<>();
            for (HudElement o : visibleElements()) {
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

        if (selected instanceof HudElement el) {
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
            mode = Mode.NONE;
            lastSnap = null;
            return true;
        }
        return false;
    }

    /** Das Mausrad ändert im Editor bewusst nichts mehr (Größe nur über die Griffe). */
    @Override public boolean mouseScrolled(double mx, double my, double amount) { return false; }

    @Override public boolean keyPressed(String key) {
        if (Keys.R.equals(key) || Keys.DELETE.equals(key) || Keys.BACKSPACE.equals(key)) {
            if (selected == null) selected = targetAt(lastRenderer, lastMouseX, lastMouseY);
            return resetSelected();
        }
        return false;
    }

    private boolean resetSelected() {
        if (selected instanceof HudElement el) {
            el.resetLayout();
            return true;
        }
        if (selected == CHAT && chat() != null) {
            chat().resetBox();
            return true;
        }
        return false;
    }

    @Override public void onClose() {
        core.hud().setEditorOpen(false);
        mode = Mode.NONE;
    }
}
