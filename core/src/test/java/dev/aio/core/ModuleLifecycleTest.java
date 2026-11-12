package dev.aio.core;

import dev.aio.core.compat.CompatMode;
import dev.aio.core.event.Events;
import dev.aio.core.modules.hud.CpsModule;
import dev.aio.core.modules.hud.FpsModule;
import dev.aio.core.modules.render.FreelookModule;
import dev.aio.core.modules.render.FullbrightModule;
import dev.aio.core.modules.render.ZoomModule;
import dev.aio.core.platform.Keys;
import dev.aio.core.platform.Perspective;
import dev.aio.core.ui.ClickGuiScreen;

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
        assertFalse(core.clicks().isListening(), "ClickTracker must not listen without users");
        // Keystrokes is off by default, CPS too now -> no mouse listener
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

        core.clientSettings().compatMode.set(CompatMode.AUTO);
        p.singleplayer = false;
        core.bus().post(new Events.ServerJoin("play.example.net:25565"));
        assertTrue(fb.isBlocked());
        assertFalse(fb.isActive());
        assertTrue(fb.isEnabled(), "the player's wish stays saved");
        assertTrue(Double.isNaN(fb.gammaOverride()));
        assertTrue(p.messages.stream().anyMatch(m -> m.contains("blocked")));
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

    @Test void compatModeIsOffByDefault() {
        FakePlatform p = new FakePlatform();
        AioCore core = newCore(p);
        assertEquals(CompatMode.OFF, core.clientSettings().compatMode.get());
        FullbrightModule fb = core.module(FullbrightModule.class);
        fb.setEnabled(true);
        p.singleplayer = false;
        core.bus().post(new Events.ServerJoin("play.example.net"));
        assertFalse(fb.isBlocked(), "Standard: nichts wird gesperrt");
        assertTrue(fb.isActive());
    }

    @Test void freelookSwitchesToThirdPersonAndBack() {
        FakePlatform p = new FakePlatform();
        AioCore core = newCore(p);
        FreelookModule fl = core.module(FreelookModule.class);
        assertTrue(fl.isActive(), "Freelook is on by default (key needed)");
        p.yaw = 90;
        p.pitch = 10;

        p.heldKeys.add(Keys.LEFT_ALT);
        core.bus().post(new Events.Key(Keys.LEFT_ALT, 1, false));
        assertTrue(fl.isEngaged());
        assertEquals(Perspective.THIRD_PERSON_BACK, p.perspective);
        assertTrue(fl.consumeMouse(100, 1000));
        assertEquals(105, fl.cameraYaw(), 1e-4);
        assertEquals(90, fl.cameraPitch(), 1e-4, "pitch is limited to 90");

        p.heldKeys.remove(Keys.LEFT_ALT);
        core.bus().post(new Events.Key(Keys.LEFT_ALT, 0, false));
        assertFalse(fl.isEngaged());
        assertEquals(Perspective.FIRST_PERSON, p.perspective);
        assertFalse(fl.consumeMouse(1, 1));
    }

    @Test void freelookReleaseMissedInMenuIsCaughtByTick() {
        FakePlatform p = new FakePlatform();
        AioCore core = newCore(p);
        FreelookModule fl = core.module(FreelookModule.class);
        p.heldKeys.add(Keys.LEFT_ALT);
        core.bus().post(new Events.Key(Keys.LEFT_ALT, 1, false));
        assertTrue(fl.isEngaged());
        p.heldKeys.remove(Keys.LEFT_ALT); // released, but the event never arrived
        core.bus().post(Events.Tick.INSTANCE);
        assertFalse(fl.isEngaged());
        assertEquals(Perspective.FIRST_PERSON, p.perspective);
    }

    @Test void freelookBlockedOnServerRestoresPerspective() {
        FakePlatform p = new FakePlatform();
        AioCore core = newCore(p);
        FreelookModule fl = core.module(FreelookModule.class);
        p.heldKeys.add(Keys.LEFT_ALT);
        core.bus().post(new Events.Key(Keys.LEFT_ALT, 1, false));
        core.clientSettings().compatMode.set(CompatMode.AUTO);
        p.singleplayer = false;
        core.bus().post(new Events.ServerJoin("mc.server.net"));
        assertFalse(fl.isEngaged());
        assertEquals(Perspective.FIRST_PERSON, p.perspective);
    }

    @Test void zoomScrollIsCancelledOnlyWhileZooming() {
        FakePlatform p = new FakePlatform();
        AioCore core = newCore(p);
        ZoomModule zoom = core.module(ZoomModule.class);
        Events.MouseScroll s1 = core.bus().post(new Events.MouseScroll(1));
        assertFalse(s1.isCancelled());
        p.heldKeys.add(Keys.C);
        core.bus().post(new Events.Key(Keys.C, 1, false));
        assertTrue(zoom.isEngaged());
        Events.MouseScroll s2 = core.bus().post(new Events.MouseScroll(1));
        assertTrue(s2.isCancelled());
        assertTrue(zoom.fovMultiplier() < 1);
    }

    @Test void zoomIsInstantThreeTimesAndScrollStepsByTenPercent() {
        FakePlatform p = new FakePlatform();
        AioCore core = newCore(p);
        ZoomModule zoom = core.module(ZoomModule.class);
        p.heldKeys.add(Keys.C);
        core.bus().post(new Events.Key(Keys.C, 1, false));
        assertEquals(1 / 3.0, zoom.fovMultiplier(), 1e-9, "immediately 3×, without transition");
        core.bus().post(new Events.MouseScroll(1));
        assertEquals(1 / 3.3, zoom.fovMultiplier(), 1e-9);
        assertTrue(zoom.sensitivityMultiplier() < 1);
        // release and press again: 3× again
        p.heldKeys.remove(Keys.C);
        core.bus().post(new Events.Key(Keys.C, 0, false));
        assertEquals(1.0, zoom.fovMultiplier(), 1e-9);
        p.heldKeys.add(Keys.C);
        core.bus().post(new Events.Key(Keys.C, 1, false));
        assertEquals(1 / 3.0, zoom.fovMultiplier(), 1e-9);
    }

    @Test void toggleKeyTogglesModuleWithMessage() {
        FakePlatform p = new FakePlatform();
        AioCore core = newCore(p);
        FpsModule fps = core.module(FpsModule.class);
        fps.toggleKey().set("key.keyboard.f");
        boolean before = fps.isEnabled();
        core.bus().post(new Events.Key("key.keyboard.f", 1, false));
        assertEquals(!before, fps.isEnabled());
        assertFalse(p.messages.isEmpty());
        // With an open screen no keybinds
        core.bus().post(new Events.Key("key.keyboard.f", 1, true));
        assertEquals(!before, fps.isEnabled());
    }

    @Test void guiKeyOpensClickGui() {
        FakePlatform p = new FakePlatform();
        AioCore core = newCore(p);
        core.bus().post(new Events.Key(Keys.RIGHT_SHIFT, 1, false));
        assertInstanceOf(ClickGuiScreen.class, p.screen);
    }
}
