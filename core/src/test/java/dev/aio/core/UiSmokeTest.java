package dev.aio.core;

import dev.aio.core.hud.HudEditorScreen;
import dev.aio.core.hud.HudElement;
import dev.aio.core.event.Events;
import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.modules.hud.FpsModule;
import dev.aio.core.platform.EffectInfo;
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
                g.keyPressed(dev.aio.core.platform.Keys.ESCAPE);
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
        editor.mouseScrolled(fps.screenX(r) + 2, fps.screenY(r) + 2, 1);
        assertTrue(fps.scaleSetting().get() > before);

        editor.onClose();
        assertFalse(core.hud().isEditorOpen());
    }

    @Test void colorModesDrawPerCharacter() {
        FakePlatform p = new FakePlatform();
        AioCore core = newCore(p);
        FpsModule fps = core.module(FpsModule.class);
        fps.setEnabled(true);
        fps.update();
        FakeRenderer r = new FakeRenderer();
        assertEquals(dev.aio.core.hud.ColorMode.CHROMA, fps.colorMode(), "Regenbogen ist Standard");
        fps.render(r, false);
        assertEquals("120 FPS".length(), r.textCalls, "Regenbogen zeichnet pro Zeichen");
        ((dev.aio.core.setting.EnumSetting<dev.aio.core.hud.ColorMode>) fps.setting("colorMode")).set(dev.aio.core.hud.ColorMode.STATIC);
        r.textCalls = 0;
        fps.render(r, false);
        assertEquals(1, r.textCalls);
    }
}
