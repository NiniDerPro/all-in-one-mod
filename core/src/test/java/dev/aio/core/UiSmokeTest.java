package dev.aio.core;

import dev.aio.core.event.Events;
import dev.aio.core.hud.ColorMode;
import dev.aio.core.hud.ColoredText;
import dev.aio.core.hud.HudEditorScreen;
import dev.aio.core.hud.HudElement;
import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.modules.chat.ChatModule;
import dev.aio.core.modules.hud.FpsModule;
import dev.aio.core.platform.EffectInfo;
import dev.aio.core.platform.Keys;
import dev.aio.core.setting.EnumSetting;
import dev.aio.core.ui.ClickGuiScreen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Zeichnet alle Screens und HUD-Elemente mit einem Fake-Renderer und klickt sie durch. */
class UiSmokeTest {
    @TempDir Path dir;

    private AioCore newCore(FakePlatform p) {
        AioCore core = new AioCore(p, dir);
        core.registerDefaultModules();
        core.start();
        return core;
    }

    @Test void allHudElementsRenderInGameAndPreview() {
        FakePlatform p = new FakePlatform();
        p.effects.add(new EffectInfo("Stärke", 1, 1800, false, true));
        AioCore core = newCore(p);
        for (Module m : core.modules().all()) m.setEnabled(true);
        FakeRenderer r = new FakeRenderer();
        core.bus().post(Events.Tick.INSTANCE);
        core.bus().post(new Events.RenderHud(r, 0));
        assertEquals(0, r.depth, "push/pop ausgeglichen");
        assertTrue(r.textCalls > 0);
        for (HudElement el : core.hud().elements()) {
            el.render(r, true);
            assertTrue(el.width(r) > 0 && el.height(r) > 0, el.id());
        }
        assertEquals(0, r.depth);
    }

    @Test void textIsMeasuredOnlyWhenItChanges() {
        FakePlatform p = new FakePlatform();
        AioCore core = newCore(p);
        FpsModule fps = core.module(FpsModule.class);
        fps.setEnabled(true);
        FakeRenderer r = new FakeRenderer();
        ColoredText.clearCache(); // Schriftprüfung jetzt, nicht zufällig mitten in der Messung
        fps.update();
        fps.render(r, false);
        int calls = r.widthCalls;
        for (int i = 0; i < 100; i++) fps.render(r, false);
        assertEquals(calls, r.widthCalls, "keine Neuberechnung pro Frame");
        p.fps = 60;
        fps.update();
        fps.render(r, false);
        assertTrue(r.widthCalls > calls);
    }

    @Test void clickGuiRendersAndHandlesClicksEverywhere() {
        FakePlatform p = new FakePlatform();
        AioCore core = newCore(p);
        ClickGuiScreen gui = new ClickGuiScreen(core, null);
        FakeRenderer r = new FakeRenderer();
        // Alle Kategorien und alle Moduleinstellungen öffnen und über das ganze Panel klicken.
        for (Category c : Category.values()) {
            for (Module m : core.modules().byCategory(c)) {
                ClickGuiScreen g = new ClickGuiScreen(core, m);
                for (int y = 0; y < r.height; y += 7) {
                    g.render(r, 200, y, 0);
                    for (int x = 130; x < r.width; x += 23) {
                        g.render(r, x, y, 0);
                        g.mouseClicked(x, y, 0);
                        g.mouseDragged(x + 5, y, 0, 5, 0);
                        g.mouseReleased(x + 5, y, 0);
                        if (p.screen != null) p.closeScreen();
                        if (core.config().profiles().size() > 3) core.config().switchProfile("default");
                    }
                    g.mouseScrolled(200, 100, -1);
                }
                g.keyPressed(Keys.ESCAPE);
                g.onClose();
            }
        }
        gui.render(r, 0, 0, 0);
        assertEquals(0, r.depth);
    }

    @Test void hudEditorDragAndScale() {
        FakePlatform p = new FakePlatform();
        AioCore core = newCore(p);
        FpsModule fps = core.module(FpsModule.class);
        fps.setEnabled(true);
        fps.update();
        HudEditorScreen editor = new HudEditorScreen(core);
        assertTrue(core.hud().isEditorOpen());
        FakeRenderer r = new FakeRenderer();
        editor.render(r, 5, 5, 0);
        float x0 = fps.screenX(r);
        assertTrue(editor.mouseClicked(x0 + 2, fps.screenY(r) + 2, 0));
        editor.mouseDragged(242, 102, 0, 0, 0);
        editor.mouseReleased(242, 102, 0);
        assertTrue(fps.screenX(r) > 100, "Element wurde verschoben");

        double before = fps.scaleSetting().get();
        assertFalse(editor.mouseScrolled(fps.screenX(r) + 2, fps.screenY(r) + 2, 1), "Mausrad ändert nichts");
        assertEquals(before, fps.scaleSetting().get());

        editor.onClose();
        assertFalse(core.hud().isEditorOpen());
    }

    /** Element im Editor auswählen (Linksklick in die Mitte) und loslassen. */
    private static void select(HudEditorScreen editor, FakeRenderer r, HudElement el) {
        float cx = el.screenX(r) + el.scaledWidth(r) / 2f, cy = el.screenY(r) + el.scaledHeight(r) / 2f;
        assertTrue(editor.mouseClicked(cx, cy, 0));
        editor.mouseReleased(cx, cy, 0);
        editor.render(r, (int) cx, (int) cy, 0);
    }

    @Test void editorResizesEdgesIndependently() {
        FakePlatform p = new FakePlatform();
        AioCore core = newCore(p);
        FpsModule fps = core.module(FpsModule.class);
        fps.setEnabled(true);
        fps.update();
        FakeRenderer r = new FakeRenderer();
        HudEditorScreen editor = new HudEditorScreen(core);
        editor.render(r, 0, 0, 0);
        fps.setScreenPos(r, 100, 100);
        select(editor, r, fps);

        // Rechte Kante: nur die Breite wächst, linke Kante bleibt stehen
        float x = fps.screenX(r), y = fps.screenY(r), w = fps.scaledWidth(r), h = fps.scaledHeight(r);
        float ex = x + w, ey = y + h / 2f;
        assertTrue(editor.mouseClicked(ex, ey, 0));
        editor.mouseDragged(ex + 20, ey + 30, 0, 0, 0);
        editor.mouseReleased(ex + 20, ey + 30, 0);
        assertEquals(w + 20, fps.scaledWidth(r), 0.6);
        assertEquals(h, fps.scaledHeight(r), 0.01, "Höhe unverändert");
        assertEquals(x, fps.screenX(r), 0.6, "linke Kante bleibt");

        // Obere linke Ecke: beide Achsen, rechte untere Ecke bleibt stehen
        x = fps.screenX(r); y = fps.screenY(r); w = fps.scaledWidth(r); h = fps.scaledHeight(r);
        assertTrue(editor.mouseClicked(x, y, 0));
        editor.mouseDragged(x - 10, y - 15, 0, 0, 0);
        editor.mouseReleased(x - 10, y - 15, 0);
        assertEquals(w + 10, fps.scaledWidth(r), 0.6);
        assertEquals(h + 15, fps.scaledHeight(r), 0.6);
        assertEquals(x + w, fps.screenX(r) + fps.scaledWidth(r), 0.6, "rechte Kante bleibt");
        assertEquals(y + h, fps.screenY(r) + fps.scaledHeight(r), 0.6, "untere Kante bleibt");

        // Shift an der Ecke behält das Seitenverhältnis
        p.heldKeys.add(Keys.LEFT_SHIFT);
        x = fps.screenX(r); y = fps.screenY(r); w = fps.scaledWidth(r); h = fps.scaledHeight(r);
        assertTrue(editor.mouseClicked(x + w, y + h, 0));
        editor.mouseDragged(x + w + 40, y + h, 0, 0, 0);
        editor.mouseReleased(x + w + 40, y + h, 0);
        assertEquals(w / h, fps.scaledWidth(r) / fps.scaledHeight(r), 0.05);
        p.heldKeys.clear();

        // Zurücksetzen über R
        assertTrue(editor.keyPressed(Keys.R));
        assertEquals(1.0, fps.scaleX(), 1e-6);
        assertEquals(1.0, fps.scaleY(), 1e-6);
    }

    @Test void editorRightClickOpensSettingsLeftClickDoesNot() {
        FakePlatform p = new FakePlatform();
        AioCore core = newCore(p);
        FpsModule fps = core.module(FpsModule.class);
        fps.setEnabled(true);
        fps.update();
        FakeRenderer r = new FakeRenderer();
        HudEditorScreen editor = new HudEditorScreen(core);
        editor.render(r, 0, 0, 0);
        fps.setScreenPos(r, 100, 100);
        float cx = fps.screenX(r) + 3, cy = fps.screenY(r) + 3;
        editor.mouseClicked(cx, cy, 0);
        editor.mouseReleased(cx, cy, 0);
        assertFalse(p.screen instanceof ClickGuiScreen, "Linksklick öffnet keine Einstellungen");
        editor.mouseClicked(cx, cy, 1);
        assertTrue(p.screen instanceof ClickGuiScreen, "Rechtsklick öffnet die Einstellungen");
    }

    @Test void chatCanBeSelectedResizedAndReset() {
        FakePlatform p = new FakePlatform();
        AioCore core = newCore(p);
        ChatModule chat = core.module(ChatModule.class);
        chat.setEnabled(true);
        core.bus().post(Events.Tick.INSTANCE);
        FakeRenderer r = new FakeRenderer();
        HudEditorScreen editor = new HudEditorScreen(core);
        editor.render(r, 0, 0, 0);
        float bottom = r.height - 40;
        float w = chat.boxWidth(), h = chat.boxHeight();
        // Auswählen
        assertTrue(editor.mouseClicked(10, bottom - 5, 0));
        editor.mouseReleased(10, bottom - 5, 0);
        editor.render(r, 10, (int) bottom - 5, 0);
        // Rechte Kante schmaler ziehen
        assertTrue(editor.mouseClicked(2 + w, bottom - h / 2f, 0));
        editor.mouseDragged(2 + w - 50, bottom - h / 2f, 0, 0, 0);
        editor.mouseReleased(2 + w - 50, bottom - h / 2f, 0);
        assertEquals(w - 50, chat.boxWidth(), 1.0);
        assertEquals(h, chat.boxHeight(), 0.01);
        // Zurücksetzen
        assertTrue(editor.keyPressed(Keys.R));
        assertEquals(1.0, chat.setting("width").get());
    }

    @Test void colorModesDrawPerCharacter() {
        FakePlatform p = new FakePlatform();
        AioCore core = newCore(p);
        FpsModule fps = core.module(FpsModule.class);
        fps.setEnabled(true);
        fps.update();
        FakeRenderer r = new FakeRenderer();
        assertEquals(ColorMode.CHROMA, fps.colorMode(), "Regenbogen ist Standard");
        fps.render(r, false);
        assertEquals("120 FPS".length(), r.textCalls, "Regenbogen zeichnet pro Zeichen");
        ((EnumSetting<ColorMode>) fps.setting("colorMode")).set(ColorMode.STATIC);
        r.textCalls = 0;
        fps.render(r, false);
        assertEquals(1, r.textCalls);
    }
}
