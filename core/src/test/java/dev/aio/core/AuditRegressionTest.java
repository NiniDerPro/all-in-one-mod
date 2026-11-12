package dev.aio.core;

import dev.aio.core.event.Events;
import dev.aio.core.modules.chat.FlexChatModule;
import dev.aio.core.modules.hud.LookAtModule;
import dev.aio.core.modules.hud.PotionStatusModule;
import dev.aio.core.modules.inventory.InventoryTweaksModule;
import dev.aio.core.modules.inventory.ItemListModule;
import dev.aio.core.modules.navigation.WaypointsModule;
import dev.aio.core.modules.render.FreelookModule;
import dev.aio.core.platform.EffectInfo;
import dev.aio.core.platform.Keys;
import dev.aio.core.platform.VanillaOption;
import dev.aio.core.setting.NumberSetting;
import dev.aio.core.setting.Setting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Tests for the findings of the audit (each one failed with the old code). */
class AuditRegressionTest {
    @TempDir Path dir;

    private AioCore start(FakePlatform p) {
        AioCore core = new AioCore(p, dir);
        core.registerDefaultModules();
        core.start();
        return core;
    }

    /** A1: At game start there are no options yet (adapter delivers NaN). */
    @Test void startsWithoutOptionsAndSyncsChatLater() {
        boolean[] ready = {false};
        FakePlatform p = new FakePlatform() {
            @Override public double option(VanillaOption o) { return ready[0] ? 0.42 : Double.NaN; }
        };
        AioCore core = assertDoesNotThrow(() -> start(p));
        FlexChatModule chat = core.module(FlexChatModule.class);
        assertTrue(chat.isActive());
        ready[0] = true;
        core.bus().post(Events.Tick.INSTANCE);
        assertEquals(0.42, (Double) chat.setting("width").get(), 1e-9, "vanilla values taken over in the first tick");
    }

    /** A1: A module that the config says is off is not briefly activated on load. */
    @Test void disabledModuleIsNotActivatedWhileLoading() {
        AioCore first = start(new FakePlatform());
        first.module(FlexChatModule.class).setEnabled(false);
        first.shutdown();
        int[] calls = {0};
        FakePlatform p = new FakePlatform() {
            @Override public double option(VanillaOption o) { calls[0]++; return 0.5; }
        };
        start(p);
        assertEquals(0, calls[0], "no access to options by a switched-off module");
    }

    /** A1: An error when activating a module does not abort the start. */
    @Test void exceptionInActivateDoesNotCrash() {
        FakePlatform p = new FakePlatform() {
            @Override public double option(VanillaOption o) { throw new IllegalStateException("kaputt"); }
        };
        AioCore core = assertDoesNotThrow(() -> start(p));
        assertFalse(core.module(FlexChatModule.class).isActive(), "faulty module detached");
        assertTrue(core.module(FlexChatModule.class).isEnabled(), "Wunsch bleibt gespeichert");
    }

    /** A2: "Reset" in the menu leaves waypoints alone. */
    @Test void resetKeepsWaypoints() {
        AioCore core = start(new FakePlatform());
        WaypointsModule wp = core.module(WaypointsModule.class);
        wp.addHere("Basis");
        for (Setting<?> s : wp.settings()) if (!s.isHidden()) s.reset(); // like the button
        assertEquals(1, wp.all().size());
    }

    /** B7: Waypoints are independent of the profile and live in their own file. */
    @Test void waypointsSurviveProfileSwitch() {
        AioCore core = start(new FakePlatform());
        WaypointsModule wp = core.module(WaypointsModule.class);
        core.config().createProfile("pvp");
        wp.addHere("Mine");
        core.config().switchProfile("default");
        assertEquals(1, wp.all().size());
        assertTrue(Files.exists(dir.resolve("waypoints.json")));
        assertFalse(core.config().exportProfile().contains("Mine"), "export contains no coordinates");
    }

    /** A2: Waypoints from old versions (stored in the profile) are taken over. */
    @Test void legacyWaypointsAreMigrated() throws Exception {
        Files.createDirectories(dir.resolve("profiles/default"));
        Files.writeString(dir.resolve("profiles/default/waypoints.json"), "{\"enabled\":true,\"settings\":{\"data\":"
                + "\"[{\\\"name\\\":\\\"Alt\\\",\\\"x\\\":1,\\\"y\\\":2,\\\"z\\\":3,\\\"world\\\":\\\"sp:Testwelt\\\",\\\"dim\\\":\\\"overworld\\\",\\\"color\\\":255}]\"}}");
        AioCore core = start(new FakePlatform());
        assertEquals(1, core.module(WaypointsModule.class).all().size());
        assertTrue(Files.readString(dir.resolve("waypoints.json")).contains("Alt"));
    }

    /** B10: Without a camera hook freelook does not intercept the mouse (otherwise the view freezes). */
    @Test void freelookDoesNotSwallowMouseWithoutHook() {
        FakePlatform p = new FakePlatform();
        p.freelookOk = false;
        AioCore core = start(p);
        FreelookModule fl = core.module(FreelookModule.class);
        p.heldKeys.add(Keys.LEFT_ALT);
        core.bus().post(new Events.Key(Keys.LEFT_ALT, 1, false));
        assertFalse(fl.consumeMouse(10, 10));
        assertTrue(p.messages.stream().anyMatch(m -> m.contains("Freelook")), "Hinweis an den Spieler");
    }

    /** B9: Own messages in server chat formats do not trigger an alert. */
    @Test void nameAlertIgnoresOwnMessagesInServerFormats() {
        FakePlatform p = new FakePlatform();
        AioCore core = start(p);
        core.bus().post(new Events.ChatReceived("[VIP] Steve: hallo Steve-Fans", true));
        core.bus().post(new Events.ChatReceived("[Admin] [Lv 5] Steve » moin", true));
        core.bus().post(new Events.ChatReceived("Steve joined the game", true));
        core.bus().post(new Events.ChatReceived("Hey Steve!", false, true));
        assertEquals(0, p.alerts);
        core.bus().post(new Events.ChatReceived("[VIP] Alex: hi Steve", true));
        assertEquals(1, p.alerts, "fremde Nachricht mit meinem Namen");
    }

    /** B13: Effects have a stable order (good before bad, then name). */
    @Test void effectsAreSorted() {
        FakePlatform p = new FakePlatform();
        p.effects.add(new EffectInfo("Vergiftung", 0, 100, false, false));
        p.effects.add(new EffectInfo("Tempo", 0, 100, false, true));
        p.effects.add(new EffectInfo("Eile", 0, 100, false, true));
        AioCore core = start(p);
        var m = core.module(PotionStatusModule.class);
        m.setEnabled(true);
        m.update();
        FakeRenderer r = new FakeRenderer();
        assertTrue(m.hasContent());
        m.render(r, false);
        assertEquals(0, r.depth);
    }

    /** E: Next to JEI/REI/EMI or Mouse Tweaks the own variants switch off. */
    @Test void conflictingModsDisableDuplicates() {
        FakePlatform p = new FakePlatform();
        AioCore core = start(p);
        assertNotNull(core.module(ItemListModule.class).panel());
        p.loadedMods.add("jei");
        assertNull(core.module(ItemListModule.class).panel());
        p.loadedMods.add("mousetweaks");
        FakeContainer c = new FakeContainer(27);
        c.set(0, "x", 5);
        assertFalse(core.module(InventoryTweaksModule.class).onScroll(c, 0, -1, false));
    }

    /** B13: The look-at info passes the configured reach on to the platform. */
    @Test void lookAtUsesConfiguredRange() {
        FakePlatform p = new FakePlatform();
        AioCore core = start(p);
        var m = core.module(LookAtModule.class);
        ((NumberSetting) m.setting("range")).set(12.0);
        m.update();
        assertEquals(12.0, p.lastLookRange);
    }
}
