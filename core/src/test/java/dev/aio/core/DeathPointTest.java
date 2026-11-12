package dev.aio.core;

import dev.aio.core.event.Events;
import dev.aio.core.modules.navigation.DeathPointModule;
import dev.aio.core.modules.navigation.Waypoint;
import dev.aio.core.modules.navigation.WaypointsModule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Death point is always saved, but only shown when the module is switched on. */
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

        assertTrue(waypoints.inThisWorld().stream().anyMatch(Waypoint::death), "death is saved even with the module off");
        assertTrue(waypoints.shownHere().stream().noneMatch(Waypoint::death), "but not shown");
        assertTrue(platform.messages.stream().noneMatch(m -> m.contains("Died")), "and no chat message");

        death.setEnabled(true);
        assertTrue(waypoints.shownHere().stream().anyMatch(Waypoint::death), "after switching on the earlier death point appears");
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
            platform.x = 5000; // far away so that nothing counts as "reached"
            core.bus().post(Events.Tick.INSTANCE);
        }
        var deaths = waypoints.inThisWorld().stream().filter(Waypoint::death).toList();
        assertEquals(3, deaths.size(), "the newest and two old ones");
        assertEquals("Death", deaths.get(2).name());
        assertEquals(300, deaths.get(2).x());
        assertEquals("Old death", deaths.get(0).name());
        assertEquals(100, deaths.get(0).x(), "the oldest (x = 0) was dropped");
    }
}
