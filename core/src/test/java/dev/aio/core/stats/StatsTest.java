package dev.aio.core.stats;

import dev.aio.core.AioCore;
import dev.aio.core.FakePlatform;
import dev.aio.core.config.ConfigManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Local statistics: counts play time and module times, survives restarts, stays local. */
class StatsTest {
    @TempDir Path dir;

    private AioCore core;

    private Stats freshStats(long[] clock, Path statsDir) {
        ConfigManager cfg = new ConfigManager(statsDir, core.modules(), () -> clock[0]);
        return new Stats(cfg, core.modules(), () -> clock[0]);
    }

    @Test void tracksPlaytimeModulesLaunchesAndPersists() throws Exception {
        core = new AioCore(new FakePlatform(), dir);
        core.registerDefaultModules();
        core.start();
        Path sdir = dir.resolve("statsdir");
        Files.createDirectories(sdir);

        long[] clock = {1_000};
        Stats s = freshStats(clock, sdir);
        s.start();
        assertEquals(1, s.launches());
        assertNotNull(s.installId());
        String id = s.installId();

        core.module(core.modules().get("zoom").getClass()).setEnabled(true);
        clock[0] += 5_000;
        s.tick();
        assertTrue(s.totalPlayMillis() >= 5_000, "play time runs with the clock");
        assertTrue(s.moduleMillis().getOrDefault("zoom", 0L) >= 5_000, "aktives Modul sammelt Zeit");
        assertTrue(s.peakActiveModules() >= 1);
        s.flush();

        // Large jump (standby) does not count as play time
        long before = s.totalPlayMillis();
        clock[0] += 60_000;
        s.tick();
        assertEquals(before, s.totalPlayMillis(), "huge jump is not counted as play time");

        // Restart: same installation id, time is kept, start count goes up
        Stats s2 = freshStats(clock, sdir);
        s2.start();
        assertEquals(id, s2.installId(), "Installations-ID bleibt");
        assertEquals(2, s2.launches());
        assertTrue(s2.totalPlayMillis() >= 5_000, "play time survives a restart");
    }

    @Test void customCountersAccumulate() throws Exception {
        core = new AioCore(new FakePlatform(), dir);
        core.registerDefaultModules();
        core.start();
        Path sdir = dir.resolve("c");
        Files.createDirectories(sdir);
        long[] clock = {0};
        Stats s = freshStats(clock, sdir);
        s.start();
        s.increment("menuOpens");
        s.increment("menuOpens");
        s.add("waypointsCreated", 3);
        s.flush();
        assertTrue(Files.readString(sdir.resolve("stats.json")).contains("menuOpens"));
    }
}
