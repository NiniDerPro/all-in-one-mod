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

    @Test void resetAllRestoresDefaults() {
        FakePlatform p = new FakePlatform();
        AioCore core = start(p);
        DateTimeModule dt = core.module(DateTimeModule.class);
        dt.setEnabled(true);
        dt.setting("seconds").fromJson(new com.google.gson.JsonPrimitive(true));
        core.module(dev.aio.core.modules.hud.ArmorHudModule.class).setEnabled(false);
        core.resetAll();
        assertFalse(dt.isEnabled());
        assertEquals(false, dt.setting("seconds").get());
        assertTrue(core.module(dev.aio.core.modules.hud.ArmorHudModule.class).isEnabled());
        assertTrue(core.clientSettings().isEnabled());
    }

    @Test void waypointDialogCreatesWaypointWithEditedCoords() {
        FakePlatform p = new FakePlatform();
        AioCore core = start(p);
        var wm = core.module(dev.aio.core.modules.navigation.WaypointsModule.class);
        wm.setEnabled(true);
        var dlg = new dev.aio.core.modules.navigation.WaypointEditScreen(core, wm, null);
        dlg.render(new FakeRenderer(), 0, 0, 0);
        try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        dlg.charTyped("H"); // Name ist anfangs markiert → ersetzt
        dlg.charTyped("i");
        dlg.keyPressed("key.keyboard.tab");
        dlg.charTyped("1");
        dlg.charTyped("2");
        dlg.keyPressed("key.keyboard.enter");
        var list = wm.inThisWorld();
        assertEquals(1, list.size());
        assertEquals("Hi", list.get(0).name());
        assertEquals(12, list.get(0).x());
    }

    @Test void otherModTakesPriorityAndBrokenHookDisablesOnlyThatModule() {
        java.util.Map<String, String> broken = new java.util.HashMap<>();
        FakePlatform p = new FakePlatform() {
            @Override public java.util.Map<String, String> conflictingMods() { return java.util.Map.of("foodinfo", "AppleSkin"); }
            @Override public java.util.Map<String, String> brokenHooks() { return broken; }
        };
        AioCore core = start(p);
        var food = core.module(dev.aio.core.modules.player.FoodInfoModule.class);
        assertTrue(food.isEnabled());
        assertFalse(food.isActive(), "AppleSkin hat Vorrang");
        assertTrue(food.unavailableReason().contains("AppleSkin"));
        core.clientSettings().preferOtherMods.set(false);
        assertTrue(food.isActive(), "abschaltbar");

        var zoom = core.module(dev.aio.core.modules.render.ZoomModule.class);
        assertTrue(zoom.isActive());
        broken.put("zoom", "conflict in CameraMixin");
        core.bus().post(new Events.ServerJoin(null));
        for (int i = 0; i < 61; i++) core.bus().post(Events.Tick.INSTANCE);
        assertFalse(zoom.isActive(), "nur Zoom aus");
        assertTrue(zoom.isEnabled(), "Einstellung bleibt");
        assertTrue(core.module(dev.aio.core.modules.render.FreelookModule.class).isActive() || !core.module(dev.aio.core.modules.render.FreelookModule.class).isEnabled());
    }
}
