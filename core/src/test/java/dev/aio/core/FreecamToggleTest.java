package dev.aio.core;

import dev.aio.core.event.Events;
import dev.aio.core.modules.render.FreecamModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FreecamToggleTest {
    @TempDir Path dir;

    @Test void f4TogglesOnAndOffAndResyncsAfterExternalStop() {
        boolean[] cam = {false};
        FakePlatform p = new FakePlatform() {
            @Override public void setFreecam(boolean on) { cam[0] = on; }
            @Override public boolean freecamActive() { return cam[0]; }
        };
        AioCore core = new AioCore(p, dir);
        core.registerDefaultModules();
        core.start();
        FreecamModule m = core.module(FreecamModule.class);
        m.setEnabled(true);
        assertFalse(cam[0], "switching on the module does not start freecam yet");
        core.bus().post(new Events.Key("key.keyboard.f4", 1, false));
        core.bus().post(new Events.Key("key.keyboard.f4", 0, false));
        assertTrue(cam[0], "1. F4: an");
        core.bus().post(Events.Tick.INSTANCE);
        assertTrue(cam[0], "stays on (toggle, not hold)");
        core.bus().post(new Events.Key("key.keyboard.f4", 1, false));
        assertFalse(cam[0], "2. F4: aus");
        // ended from outside (e.g. damage): next F4 switches on again
        core.bus().post(new Events.Key("key.keyboard.f4", 1, false));
        assertTrue(cam[0]);
        cam[0] = false;
        core.bus().post(Events.Tick.INSTANCE);
        core.bus().post(new Events.Key("key.keyboard.f4", 1, false));
        assertTrue(cam[0]);
    }
}
