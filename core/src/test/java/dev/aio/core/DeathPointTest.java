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
}
