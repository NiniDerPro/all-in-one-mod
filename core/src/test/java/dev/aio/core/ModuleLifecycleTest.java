package dev.aio.core;

import dev.aio.core.compat.CompatMode;
import dev.aio.core.event.Events;
import dev.aio.core.modules.hud.CpsModule;
import dev.aio.core.modules.hud.FpsModule;
import dev.aio.core.modules.render.FreelookModule;
import dev.aio.core.modules.render.FullbrightModule;
import dev.aio.core.modules.render.ZoomModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ModuleLifecycleTest {
    @TempDir Path dir;

    private AioCore newCore(FakePlatform p) {
        AioCore core = new AioCore(p, dir);
        core.registerDefaultModules();
        core.start();
        return core;
    }

    @Test void disabledModulesHaveNoListeners() {
        AioCore core = newCore(new FakePlatform());
        CpsModule cps = core.module(CpsModule.class);
        cps.setEnabled(false);
        assertFalse(core.clicks().isListening(), "ClickTracker darf ohne Nutzer nicht lauschen");
        // Keystrokes ist standardmäßig aus, CPS jetzt auch -> kein Maus-Listener
        int before = core.bus().listenerCount(Events.MouseButton.class);
        cps.setEnabled(true);
        assertTrue(core.clicks().isListening());
        assertTrue(core.bus().listenerCount(Events.MouseButton.class) > before);
        cps.setEnabled(false);
        assertEquals(before, core.bus().listenerCount(Events.MouseButton.class));
    }

    @Test void fpsModuleTicksOnlyWhenEnabled() {
        FakePlatform p = new FakePlatform();
        AioCore core = newCore(p);
        FpsModule fps = core.module(FpsModule.class);
        int base = core.bus().listenerCount(Events.Tick.class);
        fps.setEnabled(true);
        assertEquals(base + 1, core.bus().listenerCount(Events.Tick.class));
        fps.setEnabled(false);
        assertEquals(base, core.bus().listenerCount(Events.Tick.class));
    }

    @Test void compatModeBlocksGameplayModulesOnServers() {
        FakePlatform p = new FakePlatform();
        AioCore core = newCore(p);
        FullbrightModule fb = core.module(FullbrightModule.class);
        fb.setEnabled(true);
        assertTrue(fb.isActive());
        assertEquals(15, fb.gammaOverride());

        p.singleplayer = false;
        core.bus().post(new Events.ServerJoin("play.example.net:25565"));
        assertTrue(fb.isBlocked());
        assertFalse(fb.isActive());
        assertTrue(fb.isEnabled(), "Wunsch des Spielers bleibt gespeichert");
        assertTrue(Double.isNaN(fb.gammaOverride()));
        assertTrue(p.messages.stream().anyMatch(m -> m.contains("gesperrt")));
        assertTrue(core.module(FpsModule.class).isEnabled() || !core.module(FpsModule.class).isBlocked(),
                "HUD-Module werden nie gesperrt");

        core.clientSettings().allowList.set("example.net");
        assertFalse(fb.isBlocked());
        core.clientSettings().allowList.set("");
        assertTrue(fb.isBlocked());

        core.bus().post(Events.ServerLeave.INSTANCE);
        assertFalse(fb.isBlocked());
        assertTrue(fb.isActive());

        core.clientSettings().compatMode.set(CompatMode.ALWAYS);
        assertTrue(fb.isBlocked());
    }

    @Test void freelookSwitchesToThirdPersonAndBack() {
        FakePlatform p = new FakePlatform();
        AioCore core = newCore(p);
        FreelookModule fl = core.module(FreelookModule.class);
        assertTrue(fl.isActive(), "Freelook ist standardmäßig an (Taste nötig)");
        p.yaw = 90;
        p.pitch = 10;

        p.heldKeys.add(342);
        core.bus().post(new Events.Key(342, 1, 0, false));
        assertTrue(fl.isEngaged());
        assertEquals(dev.aio.core.platform.Perspective.THIRD_PERSON_BACK, p.perspective);
        assertTrue(fl.consumeMouse(100, 1000));
        assertEquals(105, fl.cameraYaw(), 1e-4);
        assertEquals(90, fl.cameraPitch(), 1e-4, "Pitch wird auf 90 begrenzt");

        p.heldKeys.remove(342);
        core.bus().post(new Events.Key(342, 0, 0, false));
        assertFalse(fl.isEngaged());
        assertEquals(dev.aio.core.platform.Perspective.FIRST_PERSON, p.perspective);
        assertFalse(fl.consumeMouse(1, 1));
    }

    @Test void freelookReleaseMissedInMenuIsCaughtByTick() {
        FakePlatform p = new FakePlatform();
        AioCore core = newCore(p);
        FreelookModule fl = core.module(FreelookModule.class);
        p.heldKeys.add(342);
        core.bus().post(new Events.Key(342, 1, 0, false));
        assertTrue(fl.isEngaged());
        p.heldKeys.remove(342); // losgelassen, aber Event nie angekommen
        core.bus().post(Events.Tick.INSTANCE);
        assertFalse(fl.isEngaged());
        assertEquals(dev.aio.core.platform.Perspective.FIRST_PERSON, p.perspective);
    }

    @Test void freelookBlockedOnServerRestoresPerspective() {
        FakePlatform p = new FakePlatform();
        AioCore core = newCore(p);
        FreelookModule fl = core.module(FreelookModule.class);
        p.heldKeys.add(342);
        core.bus().post(new Events.Key(342, 1, 0, false));
        p.singleplayer = false;
        core.bus().post(new Events.ServerJoin("mc.server.net"));
        assertFalse(fl.isEngaged());
        assertEquals(dev.aio.core.platform.Perspective.FIRST_PERSON, p.perspective);
    }

    @Test void zoomScrollIsCancelledOnlyWhileZooming() {
        FakePlatform p = new FakePlatform();
        AioCore core = newCore(p);
        ZoomModule zoom = core.module(ZoomModule.class);
        Events.MouseScroll s1 = core.bus().post(new Events.MouseScroll(1));
        assertFalse(s1.isCancelled());
        p.heldKeys.add(67);
        core.bus().post(new Events.Key(67, 1, 0, false));
        assertTrue(zoom.isEngaged());
        Events.MouseScroll s2 = core.bus().post(new Events.MouseScroll(1));
        assertTrue(s2.isCancelled());
        assertTrue(zoom.fovMultiplier() < 1);
    }

    @Test void toggleKeyTogglesModuleWithMessage() {
        FakePlatform p = new FakePlatform();
        AioCore core = newCore(p);
        FpsModule fps = core.module(FpsModule.class);
        fps.toggleKey().set(70);
        boolean before = fps.isEnabled();
        core.bus().post(new Events.Key(70, 1, 0, false));
        assertEquals(!before, fps.isEnabled());
        assertFalse(p.messages.isEmpty());
        // Mit offenem Screen keine Keybinds
        core.bus().post(new Events.Key(70, 1, 0, true));
        assertEquals(!before, fps.isEnabled());
    }

    @Test void guiKeyOpensClickGui() {
        FakePlatform p = new FakePlatform();
        AioCore core = newCore(p);
        core.bus().post(new Events.Key(344, 1, 0, false));
        assertInstanceOf(dev.aio.core.ui.ClickGuiScreen.class, p.screen);
    }
}
