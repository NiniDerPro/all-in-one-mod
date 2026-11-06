package dev.aio.core.modules.navigation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SharedWaypointTest {
    @Test void parsesTheChatFormat() {
        var l = SharedWaypoint.parse("<Steve> xaero-waypoint:Base:B:100:64:-200:4:false:0:Internal-overworld-waypoints");
        assertEquals(1, l.size());
        var f = l.get(0);
        assertEquals("Base", f.name());
        assertEquals(100, f.x());
        assertEquals(64, f.y());
        assertEquals(-200, f.z());
        assertEquals(0xAA0000, f.color());
        assertEquals("overworld", f.dimension());
    }

    @Test void handlesTildeNetherAndGarbage() {
        var f = SharedWaypoint.parse("xaero-waypoint:Portal:P:12:~:34:9:false:0:Internal-the_nether-waypoints").get(0);
        assertNull(f.y());
        assertEquals("the_nether", f.dimension());
        assertTrue(SharedWaypoint.parse("xaero-waypoint:kaputt").isEmpty());
        assertTrue(SharedWaypoint.parse("hello world").isEmpty());
    }
}
