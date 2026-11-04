package dev.aio.core;

import dev.aio.core.event.Events;
import dev.aio.core.hud.HudPart;
import dev.aio.core.modules.info.DateTimeModule;
import dev.aio.core.modules.render.FreecamModule;
import dev.aio.core.modules.render.FreelookModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class HotkeysAndPartsTest {
    @TempDir Path dir;

    private AioCore start(FakePlatform p) {
        AioCore core = new AioCore(p, dir);
        core.registerDefaultModules();
        core.start();
        return core;
    }

    @Test void f3PlusF4DoesNotToggleFreecam() {
        boolean[] cam = {false};
        FakePlatform p = new FakePlatform() {
            @Override public void setFreecam(boolean on) { cam[0] = on; }
            @Override public boolean freecamActive() { return cam[0]; }
        };
        AioCore core = start(p);
        core.module(FreecamModule.class).setEnabled(true);
        p.heldKeys.add("key.keyboard.f3");
        core.bus().post(new Events.Key("key.keyboard.f4", 1, false));
        assertFalse(cam[0], "F3+F4 ist nicht F4");
        p.heldKeys.remove("key.keyboard.f3");
        core.bus().post(new Events.Key("key.keyboard.f4", 1, false));
        assertTrue(cam[0], "F4 allein schaltet");
    }

    @Test void altTabEndsHeldFreelook() {
        FakePlatform p = new FakePlatform();
        AioCore core = start(p);
        FreelookModule fl = core.module(FreelookModule.class);
        fl.setEnabled(true);
        p.heldKeys.add("key.keyboard.left.alt");
        core.bus().post(new Events.Key("key.keyboard.left.alt", 1, false));
        assertTrue(fl.isEngaged(), "Alt allein: Freelook");
        core.bus().post(new Events.Key("key.keyboard.tab", 1, false));
        assertFalse(fl.isEngaged(), "Alt+Tab: sofort beendet");
    }

    @Test void dateTimeHasMovableParts() {
        FakePlatform p = new FakePlatform();
        AioCore core = start(p);
        DateTimeModule dt = core.module(DateTimeModule.class);
        dt.setEnabled(true);
        dt.update();
        assertTrue(dt.timeText().matches("\\d\\d:\\d\\d"), dt.timeText());
        assertTrue(dt.dateText().matches("\\d\\d\\.\\d\\d\\.\\d{4}"), dt.dateText());
        assertTrue(dt.separated());
        FakeRenderer r = new FakeRenderer();
        HudPart time = dt.parts().get(0), date = dt.parts().get(1);
        assertTrue(date.screenY(r) > time.screenY(r), "Datum startet unter der Uhrzeit");
        date.setScreenPos(r, 10, 10);
        assertEquals(10, date.screenX(r), 0.5);
        assertNotEquals(10, time.screenX(r), 0.5, "Uhrzeit bleibt, wo sie war");
        dt.render(r, true);
    }
}
