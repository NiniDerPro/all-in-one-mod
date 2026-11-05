package dev.aio.core;

import dev.aio.core.event.Events;
import dev.aio.core.modules.navigation.DeathPointModule;
import dev.aio.core.modules.navigation.Waypoint;
import dev.aio.core.modules.navigation.WaypointsModule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Todespunkt wird immer gespeichert, aber nur bei eingeschaltetem Modul angezeigt. */
class DeathPointTest {
    @TempDir Path dir;

    @Test void deathIsRecordedWhileOffAndShownOnceTurnedOn() {
        FakePlatform platform = new FakePlatform();
        AioCore core = new AioCore(platform, dir);
        core.registerDefaultModules();
        core.start();
        DeathPointModule death = core.module(DeathPointModule.class);
        WaypointsModule waypoints = core.module(WaypointsModule.class);
        death.setEnabled(false);
        waypoints.setEnabled(true);

        platform.dead = true;
        core.bus().post(Events.Tick.INSTANCE);
        platform.dead = false;
        platform.x = 500; // weit weg vom Todespunkt
        core.bus().post(Events.Tick.INSTANCE);

        assertTrue(waypoints.inThisWorld().stream().anyMatch(Waypoint::death), "Tod wird auch bei ausgeschaltetem Modul gespeichert");
        assertTrue(waypoints.shownHere().stream().noneMatch(Waypoint::death), "aber nicht angezeigt");
        assertTrue(platform.messages.stream().noneMatch(m -> m.contains("Died")), "und keine Chatmeldung");

        death.setEnabled(true);
        assertTrue(waypoints.shownHere().stream().anyMatch(Waypoint::death), "nach dem Einschalten erscheint der frühere Todespunkt");
    }

    @Test void keepsTheLastThreeDeaths() {
        FakePlatform platform = new FakePlatform();
        AioCore core = new AioCore(platform, dir);
        core.registerDefaultModules();
        core.start();
        core.module(DeathPointModule.class).setEnabled(true);
        WaypointsModule waypoints = core.module(WaypointsModule.class);
        for (int i = 0; i < 4; i++) {
            platform.x = 100 * i;
            platform.dead = true;
            core.bus().post(Events.Tick.INSTANCE);
            platform.dead = false;
            platform.x = 5000; // weit weg, damit nichts als "erreicht" gilt
            core.bus().post(Events.Tick.INSTANCE);
        }
        var deaths = waypoints.inThisWorld().stream().filter(Waypoint::death).toList();
        assertEquals(3, deaths.size(), "der neueste und zwei alte");
        assertEquals("Death", deaths.get(2).name());
        assertEquals(300, deaths.get(2).x());
        assertEquals("Old death", deaths.get(0).name());
        assertEquals(100, deaths.get(0).x(), "der älteste (x = 0) ist weggefallen");
    }
}
