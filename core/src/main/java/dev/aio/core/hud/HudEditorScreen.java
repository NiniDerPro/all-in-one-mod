package dev.aio.core.hud;

import dev.aio.core.AioCore;
import dev.aio.core.platform.Keys;
import dev.aio.core.platform.Renderer;
import dev.aio.core.ui.Theme;
import dev.aio.core.ui.UiScreen;

import java.util.ArrayList;
import java.util.List;

/**
 * HUD-Editor: Elemente per Drag &amp; Drop verschieben, mit dem Mausrad skalieren,
 * Einrasten an Rändern und anderen Elementen (Shift gedrückt halten = frei verschieben).
 * Rechtsklick öffnet die Einstellungen des Elements, R setzt es zurück.
 */
public final class HudEditorScreen implements UiScreen {
    private static final float SNAP_THRESHOLD = 5f;

    private final AioCore core;
    private HudElement dragging;
    private float dragOffX, dragOffY;
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
        for (HudElement el : core.hud().elements()) if (el.isActive()) list.add(el);
        return list;
    }

    @Override public void render(Renderer r, int mouseX, int mouseY, float delta) {
        lastRenderer = r;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        int w = r.screenWidth(), h = r.screenHeight();
        r.fill(0, 0, w, h, 0x30000000);

        // Mittellinien dezent
        r.fill(w / 2f - 0.5f, 0, w / 2f + 0.5f, h, 0x18FFFFFF);
        r.fill(0, h / 2f - 0.5f, w, h / 2f + 0.5f, 0x18FFFFFF);

        HudElement hovered = dragging != null ? dragging : elementAt(r, mouseX, mouseY);
        for (HudElement el : visibleElements()) {
            el.update();
            el.render(r, true);
            float x = el.screenX(r), y = el.screenY(r);
            int color = el == hovered ? Theme.accent(core) : 0x60FFFFFF;
            r.outline(x - 1, y - 1, x + el.scaledWidth(r) + 1, y + el.scaledHeight(r) + 1, 1, color);
        }

        if (dragging != null && lastSnap != null) {
            int guide = Theme.accent(core);
            if (lastSnap.hasGuideX()) r.fill(lastSnap.guideX() - 0.5f, 0, lastSnap.guideX() + 0.5f, h, guide);
            if (lastSnap.hasGuideY()) r.fill(0, lastSnap.guideY() - 0.5f, w, lastSnap.guideY() + 0.5f, guide);
        }

        if (hovered != null && dragging == null) {
            String label = hovered.name() + "  " + Math.round(hovered.scaleSetting().get() * 100) + "%";
            float lx = Math.min(mouseX + 8, w - r.textWidth(label) - 4);
            float ly = Math.min(mouseY + 8, h - 12);
            r.fill(lx - 3, ly - 3, lx + r.textWidth(label) + 3, ly + r.fontHeight() + 1, 0xC0101014);
            r.text(label, lx, ly, 0xFFFFFFFF, false);
        }

        String hint = "Ziehen: verschieben · Mausrad: Größe · Shift: frei · Rechtsklick: Einstellungen · R: zurücksetzen";
        float hy = h - 14;
        if (r.textWidth(hint) > w - 8) hint = "Ziehen · Mausrad · Shift · Rechtsklick · R";
        r.centeredText(hint, w / 2f, hy, 0xB0FFFFFF, true);

        drawButton(r, settingsButton(r), "Einstellungen", mouseX, mouseY);
    }

    private float[] settingsButton(Renderer r) {
        float bw = r.textWidth("Einstellungen") + 16;
        float x = r.screenWidth() / 2f - bw / 2f;
        return new float[]{x, 6, x + bw, 22};
    }

    private void drawButton(Renderer r, float[] b, String label, int mx, int my) {
        boolean hover = mx >= b[0] && mx <= b[2] && my >= b[1] && my <= b[3];
        r.fill(b[0], b[1], b[2], b[3], hover ? Theme.accent(core) : 0xD0181820);
        r.centeredText(label, (b[0] + b[2]) / 2f, b[1] + 4, 0xFFFFFFFF, false);
    }

    private HudElement elementAt(Renderer r, double mx, double my) {
        if (r == null) return null;
        List<HudElement> list = visibleElements();
        for (int i = list.size() - 1; i >= 0; i--) {
            HudElement el = list.get(i);
            float x = el.screenX(r), y = el.screenY(r);
            if (mx >= x && mx <= x + el.scaledWidth(r) && my >= y && my <= y + el.scaledHeight(r)) return el;
        }
        return null;
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        Renderer r = lastRenderer;
        if (r == null) return false;
        float[] b = settingsButton(r);
        if (button == 0 && mx >= b[0] && mx <= b[2] && my >= b[1] && my <= b[3]) {
            core.openClickGui(null);
            return true;
        }
        HudElement el = elementAt(r, mx, my);
        if (el == null) return false;
        if (button == 0) {
            dragging = el;
            dragOffX = (float) mx - el.screenX(r);
            dragOffY = (float) my - el.screenY(r);
            lastSnap = null;
            return true;
        }
        if (button == 1) {
            core.openClickGui(el);
            return true;
        }
        return false;
    }

    @Override public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        Renderer r = lastRenderer;
        if (dragging == null || r == null) return false;
        float nx = (float) mx - dragOffX;
        float ny = (float) my - dragOffY;
        float w = dragging.scaledWidth(r), h = dragging.scaledHeight(r);
        if (core.platform().isKeyDown(Keys.LEFT_SHIFT)) {
            lastSnap = new Snapper.Result(
                    Math.max(0, Math.min(r.screenWidth() - w, nx)),
                    Math.max(0, Math.min(r.screenHeight() - h, ny)), Float.NaN, Float.NaN);
        } else {
            List<Snapper.Rect> others = new ArrayList<>();
            for (HudElement o : visibleElements()) {
                if (o != dragging) others.add(new Snapper.Rect(o.screenX(r), o.screenY(r), o.scaledWidth(r), o.scaledHeight(r)));
            }
            lastSnap = Snapper.snap(new Snapper.Rect(nx, ny, w, h), others, r.screenWidth(), r.screenHeight(), SNAP_THRESHOLD);
        }
        dragging.setScreenPos(r, lastSnap.x(), lastSnap.y());
        return true;
    }

    @Override public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0 && dragging != null) {
            dragging = null;
            lastSnap = null;
            return true;
        }
        return false;
    }

    @Override public boolean mouseScrolled(double mx, double my, double amount) {
        Renderer r = lastRenderer;
        HudElement el = elementAt(r, mx, my);
        if (el == null || amount == 0) return false;
        // Um die Mitte herum skalieren
        float cx = el.screenX(r) + el.scaledWidth(r) / 2f;
        float cy = el.screenY(r) + el.scaledHeight(r) / 2f;
        el.scaleSetting().set(el.scaleSetting().get() + (amount > 0 ? 0.05 : -0.05));
        el.setScreenPos(r, cx - el.scaledWidth(r) / 2f, cy - el.scaledHeight(r) / 2f);
        return true;
    }

    @Override public boolean keyPressed(int key, int scancode, int modifiers) {
        if (key == 82 /* R */) {
            HudElement el = elementAt(lastRenderer, lastMouseX, lastMouseY);
            if (el != null) {
                el.scaleSetting().reset();
                el.setting("x").reset();
                el.setting("y").reset();
                return true;
            }
        }
        return false;
    }

    @Override public void onClose() {
        core.hud().setEditorOpen(false);
        dragging = null;
    }
}
