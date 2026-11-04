package dev.aio.core;

import dev.aio.core.compat.CompatMode;
import dev.aio.core.compat.ServerCompat;
import dev.aio.core.event.EventBus;
import dev.aio.core.event.Events;
import dev.aio.core.hud.Snapper;
import dev.aio.core.setting.ColorSetting;
import dev.aio.core.setting.NumberSetting;
import dev.aio.core.util.Animation;
import dev.aio.core.util.ClickTracker;
import dev.aio.core.util.Colors;
import dev.aio.core.util.RollingStats;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UtilTest {
    @Test void clickTrackerDropsClicksOlderThanOneSecond() {
        long[] now = {0};
        EventBus bus = new EventBus();
        ClickTracker t = new ClickTracker(bus, () -> now[0]);
        t.acquire();
        for (int i = 0; i < 5; i++) {
            bus.post(new Events.MouseButton(0, 1, false));
            now[0] += 100_000_000L;
        }
        bus.post(new Events.MouseButton(1, 1, false));
        bus.post(new Events.MouseButton(0, 0, false)); // Loslassen zählt nicht
        bus.post(new Events.MouseButton(0, 1, true));  // Klick im Menü zählt nicht
        assertEquals(5, t.leftCps());
        assertEquals(1, t.rightCps());
        now[0] = 1_000_000_001L; // erster Klick (t=0) ist jetzt > 1 s alt
        assertEquals(4, t.leftCps());
        now[0] += 2_000_000_000L;
        assertEquals(0, t.leftCps());
        assertEquals(0, t.rightCps());
    }

    @Test void clickTrackerIsRefCounted() {
        EventBus bus = new EventBus();
        ClickTracker t = new ClickTracker(bus, System::nanoTime);
        t.acquire();
        t.acquire();
        t.release();
        assertTrue(t.isListening());
        t.release();
        assertFalse(t.isListening());
        assertFalse(bus.hasListeners(Events.MouseButton.class));
        t.release(); // zu viele releases sind harmlos
    }

    @Test void snapsToScreenEdgesCenterAndOthers() {
        Snapper.Result r = Snapper.snap(new Snapper.Rect(3, 50, 40, 10), List.of(), 400, 300, 5);
        assertEquals(0, r.x());
        assertEquals(0, r.guideX());
        assertFalse(r.hasGuideY());

        r = Snapper.snap(new Snapper.Rect(178, 100, 40, 10), List.of(), 400, 300, 5);
        assertEquals(180, r.x(), "Mitte rastet auf Bildschirmmitte ein");

        Snapper.Rect other = new Snapper.Rect(100, 100, 50, 20);
        r = Snapper.snap(new Snapper.Rect(153, 123, 30, 10), List.of(other), 400, 300, 5);
        assertEquals(150, r.x(), "linke Kante an rechte Kante des anderen");
        assertEquals(120, r.y(), "obere Kante an untere Kante des anderen");

        r = Snapper.snap(new Snapper.Rect(500, -20, 40, 10), List.of(), 400, 300, 5);
        assertEquals(360, r.x(), "bleibt im Bildschirm");
        assertEquals(0, r.y());
    }

    @Test void compatAllowListMatching() {
        assertTrue(ServerCompat.isAllowed("play.example.net:25565", "example.net"));
        assertTrue(ServerCompat.isAllowed("EXAMPLE.net.", " foo.org , example.net"));
        assertFalse(ServerCompat.isAllowed("notexample.net", "example.net"));
        assertFalse(ServerCompat.isAllowed("example.net", ""));
        assertFalse(ServerCompat.shouldBlock(CompatMode.AUTO, true, null, ""));
        assertTrue(ServerCompat.shouldBlock(CompatMode.AUTO, false, "a.b", ""));
        assertFalse(ServerCompat.shouldBlock(CompatMode.OFF, false, "a.b", ""));
        assertTrue(ServerCompat.shouldBlock(CompatMode.ALWAYS, true, null, ""));
    }

    @Test void numberSettingClampsAndSteps() {
        NumberSetting n = new NumberSetting("n", "N", 1, 0.5, 3, 0.05);
        n.set(1.234);
        assertEquals(1.25, n.get(), 1e-9);
        n.set(10.0);
        assertEquals(3.0, n.get());
        n.set(Double.NaN);
        assertEquals(3.0, n.get());
        n.setFraction(0);
        assertEquals(0.5, n.get());
    }

    @Test void colorSettingParsesHex() {
        ColorSetting c = new ColorSetting("c", "C", 0);
        c.fromJson(new JsonPrimitive("#80FF0000"));
        assertEquals(0x80FF0000, c.argb());
        c.fromJson(new JsonPrimitive("00FF00"));
        assertEquals(0xFF00FF00, c.argb());
        c.fromJson(new JsonPrimitive("zz"));
        assertEquals(0xFF00FF00, c.argb());
        c.setChannel(24, 16);
        assertEquals(0x1000FF00, c.argb());
        assertEquals("\"#1000FF00\"", c.toJson().toString());
    }

    @Test void mouseKeyNamesRoundTrip() {
        for (int b = 0; b < 8; b++) assertEquals(b, dev.aio.core.platform.Keys.mouseButton(dev.aio.core.platform.Keys.mouse(b)));
        assertEquals("key.mouse.left", dev.aio.core.platform.Keys.mouse(0));
        assertEquals("key.mouse.4", dev.aio.core.platform.Keys.mouse(3));
        assertEquals(-1, dev.aio.core.platform.Keys.mouseButton("key.keyboard.a"));
    }

    @Test void hsvAndChroma() {
        assertEquals(0xFFFF0000, Colors.hsv(0, 1, 1, 255));
        assertEquals(0xFF00FF00, Colors.hsv(1 / 3f, 1, 1, 255));
        assertEquals(0xFF0000FF, Colors.hsv(2 / 3f, 1, 1, 255));
        assertEquals(0xFFFF0000, Colors.hsv(1f, 1, 1, 255), "Farbton ist zyklisch");
        assertEquals(0x80, Colors.alpha(Colors.chroma(10, 1, 0x80)));
    }

    @Test void rollingStats() {
        RollingStats s = new RollingStats(3);
        s.add(10); s.add(20); s.add(30); s.add(60);
        assertEquals(3, s.size());
        assertEquals(20, s.min());
        assertEquals(60, s.max());
        assertEquals(37, s.average());
    }

    @Test void colorsAndAnimation() {
        assertEquals(0xFF808080, Colors.lerp(0xFF000000, 0xFFFFFFFF, 0.5f));
        Animation a = new Animation();
        assertEquals(1f, a.update(1f, 100, 0));         // erster Aufruf springt
        assertEquals(0.5f, a.update(0f, 100, 50_000_000L), 1e-4);
        assertEquals(0f, a.update(0f, 100, 200_000_000L), 1e-4);
    }
}
