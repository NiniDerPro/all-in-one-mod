package dev.aio.core;

import dev.aio.core.event.Events;
import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.modules.chat.ChatModule;
import dev.aio.core.modules.combat.AutoToolModule;
import dev.aio.core.modules.combat.AutoWeaponModule;
import dev.aio.core.modules.info.PlaytimeModule;
import dev.aio.core.modules.info.WorldTime;
import dev.aio.core.modules.inventory.InventoryOps;
import dev.aio.core.modules.inventory.InventoryTweaksModule;
import dev.aio.core.modules.inventory.ItemSearch;
import dev.aio.core.modules.navigation.Compass;
import dev.aio.core.modules.navigation.DeathPointModule;
import dev.aio.core.modules.navigation.Projection;
import dev.aio.core.modules.navigation.Waypoint;
import dev.aio.core.modules.navigation.WaypointsModule;
import dev.aio.core.platform.GameKey;
import dev.aio.core.platform.ItemRef;
import dev.aio.core.platform.VanillaOption;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FeaturesTest {
    @TempDir Path dir;

    private AioCore newCore(FakePlatform p) {
        AioCore core = new AioCore(p, dir);
        core.registerDefaultModules();
        core.start();
        return core;
    }

    @Test void everyCategoryUsedAndIdsUnique() {
        AioCore core = newCore(new FakePlatform());
        for (Category c : Category.values()) assertFalse(core.modules().byCategory(c).isEmpty(), c.name());
        assertTrue(core.modules().all().size() >= 35);
    }

    @Test void worldTime() {
        assertEquals("06:00", WorldTime.format(0, false));
        assertEquals("12:00", WorldTime.format(6000, false));
        assertEquals("00:00", WorldTime.format(18000, false));
        assertEquals("6:30 PM", WorldTime.format(12500, true));
        assertEquals(1, WorldTime.day(0));
        assertEquals(3, WorldTime.day(48000));
        assertTrue(WorldTime.isNight(18000));
        assertFalse(WorldTime.isNight(6000));
    }

    @Test void playtimeFormat() {
        assertEquals("0 min", PlaytimeModule.format(0));
        assertEquals("12 min", PlaytimeModule.format(20 * 60 * 12));
        assertEquals("3 h 5 min", PlaytimeModule.format(20L * 60 * (3 * 60 + 5)));
        assertEquals("2 d 1 h 0 min", PlaytimeModule.format(20L * 60 * (2 * 24 * 60 + 60)));
    }

    @Test void playtimeRequestsStatsAndCountsUp() {
        FakePlatform p = new FakePlatform();
        AioCore core = newCore(p);
        PlaytimeModule m = core.module(PlaytimeModule.class);
        m.setEnabled(true);
        for (int i = 0; i < 40; i++) core.bus().post(Events.Tick.INSTANCE);
        assertEquals(1, p.statRequests, "Statistik wird angefragt");
        p.playTime = 20 * 60 * 61;
        core.bus().post(Events.Tick.INSTANCE);
        m.update();
        assertTrue(m.hasContent());
    }

    @Test void compassMath() {
        assertEquals(0, Compass.heading(180), 1e-4);   // Norden
        assertEquals(90, Compass.heading(-90), 1e-4);  // Osten
        assertEquals(180, Compass.heading(0), 1e-4);   // Süden
        assertEquals(270, Compass.heading(90), 1e-4);  // Westen
        assertEquals(0, Compass.bearing(0, -10), 1e-4);
        assertEquals(90, Compass.bearing(10, 0), 1e-4);
        assertEquals(-10, Compass.delta(5, 355), 1e-4);
        assertEquals("NE", Compass.label(45));
        assertNull(Compass.label(30));
    }

    @Test void projectionCenterAndSides() {
        // Blick nach +Z (Yaw 0): Punkt geradeaus = Bildmitte
        Projection.Point p = Projection.project(0, 0, 10, 0, 0, 0, 0, 0, 70, 400, 200);
        assertTrue(p.onScreen());
        assertEquals(200, p.x(), 0.01);
        assertEquals(100, p.y(), 0.01);
        // Blick nach Süden: Westen (-X) liegt rechts
        assertTrue(Projection.project(-3, 0, 10, 0, 0, 0, 0, 0, 70, 400, 200).x() > 200);
        // oben = kleineres y
        assertTrue(Projection.project(0, 3, 10, 0, 0, 0, 0, 0, 70, 400, 200).y() < 100);
        // hinter der Kamera
        assertTrue(Projection.project(0, 0, -10, 0, 0, 0, 0, 0, 70, 400, 200).behind());
    }

    @Test void waypointsPersistPerWorldAndDimension() {
        FakePlatform p = new FakePlatform();
        AioCore core = newCore(p);
        WaypointsModule wp = core.module(WaypointsModule.class);
        wp.addHere("Basis");
        p.dimension = "the_nether";
        wp.addHere("Portal");
        p.worldKey = "server:anders";
        wp.addHere("Fremd");
        core.shutdown();

        AioCore again = newCore(new FakePlatform());
        WaypointsModule w2 = again.module(WaypointsModule.class);
        assertEquals(3, w2.all().size());
        assertEquals(List.of("Basis", "Portal"), w2.inThisWorld().stream().map(Waypoint::name).toList());
        assertEquals(List.of("Basis"), w2.visibleHere().stream().map(Waypoint::name).toList());
    }

    @Test void waypointKeyOpensCreateDialog() {
        FakePlatform p = new FakePlatform();
        AioCore core = newCore(p);
        core.module(WaypointsModule.class).setEnabled(true);
        core.bus().post(new Events.Key("key.keyboard.b", 1, false));
        assertInstanceOf(dev.aio.core.modules.navigation.WaypointEditScreen.class, p.screen);
        assertEquals(0, core.module(WaypointsModule.class).inThisWorld().size(), "erst beim Speichern");
    }

    @Test void deathPointCreatedAndRemovedOnArrival() {
        FakePlatform p = new FakePlatform();
        AioCore core = newCore(p);
        WaypointsModule wp = core.module(WaypointsModule.class);
        wp.setEnabled(true);
        core.module(DeathPointModule.class).setEnabled(true);
        assertTrue(core.module(DeathPointModule.class).isActive());
        p.x = 100; p.z = 200;
        p.dead = true;
        core.bus().post(Events.Tick.INSTANCE);
        assertEquals(1, wp.inThisWorld().size());
        assertTrue(wp.inThisWorld().get(0).death());
        p.dead = false;
        p.x = 0; p.z = 0; // Respawn weit weg
        core.bus().post(Events.Tick.INSTANCE);
        assertEquals(1, wp.inThisWorld().size(), "bleibt bis zur Ankunft");
        p.x = 101; p.z = 200;
        core.bus().post(Events.Tick.INSTANCE);
        assertTrue(wp.inThisWorld().isEmpty(), "verschwindet bei Ankunft");
    }

    @Test void nameAlert() {
        FakePlatform p = new FakePlatform();
        AioCore core = newCore(p);
        core.bus().post(new Events.ChatReceived("<Alex> hey steve, komm mal", false));
        assertEquals(1, p.alerts);
        core.bus().post(new Events.ChatReceived("<Alex> @Steve schau", false));
        assertEquals(2, p.alerts);
        core.bus().post(new Events.ChatReceived("<Alex> Steven ist da", false));
        assertEquals(2, p.alerts, "nur ganze Wörter");
        core.bus().post(new Events.ChatReceived("<Steve> ich bin Steve", false));
        assertEquals(2, p.alerts, "eigene Nachricht ignoriert");
    }

    @Test void chatReadsVanillaOptionsAndWritesOnlyOnChange() {
        FakePlatform p = new FakePlatform();
        p.options.put(VanillaOption.CHAT_WIDTH, 0.3);
        p.options.put(VanillaOption.CHAT_SCALE, 1.0);
        p.options.put(VanillaOption.CHAT_HEIGHT_FOCUSED, 0.2);
        AioCore core = newCore(p);
        ChatModule chat = core.module(ChatModule.class);
        assertTrue(chat.isActive());
        assertEquals(0.3, chat.setting("width").get(), "übernimmt Vanilla-Wert");
        assertEquals(0.3, p.options.get(VanillaOption.CHAT_WIDTH));
        chat.resizeTo(40 + 280 * 0.8f, 20 + 160 * 0.5f);
        assertEquals(0.8, p.options.get(VanillaOption.CHAT_WIDTH), 0.01);
        assertEquals(0.5, p.options.get(VanillaOption.CHAT_HEIGHT_FOCUSED), 0.05, "auf ganze Zeilen gerundet");
    }

    @Test void chatHistoryCollectsMessages() {
        FakePlatform p = new FakePlatform();
        AioCore core = newCore(p);
        core.bus().post(new Events.ChatReceived("Hallo Welt", false));
        core.bus().post(new Events.ChatReceived("Zweite", true));
        assertEquals(2, core.module(ChatModule.class).chatHistory().entries().size());
    }

    @Test void autoToolSwitchesAndRestores() {
        FakePlatform p = new FakePlatform();
        AioCore core = newCore(p);
        AutoToolModule m = core.module(AutoToolModule.class);
        m.setEnabled(true);
        p.selected = 2;
        p.bestTool = 5;
        p.heldGameKeys.add(GameKey.ATTACK);
        core.bus().post(new Events.StartBreak(0, 0, 0));
        assertEquals(5, p.selected);
        core.bus().post(Events.Tick.INSTANCE);
        assertEquals(5, p.selected, "bleibt, solange abgebaut wird");
        p.heldGameKeys.remove(GameKey.ATTACK);
        core.bus().post(Events.Tick.INSTANCE);
        assertEquals(5, p.selected, "Standard: bleibt beim Werkzeug");

        // Mit "Switch back afterwards" geht es zum alten Slot
        ((dev.aio.core.setting.BooleanSetting) m.setting("restore")).set(true);
        p.selected = 2;
        p.heldGameKeys.add(GameKey.ATTACK);
        core.bus().post(new Events.StartBreak(0, 0, 0));
        p.heldGameKeys.remove(GameKey.ATTACK);
        core.bus().post(Events.Tick.INSTANCE);
        assertEquals(2, p.selected, "zurück zum alten Slot");
    }

    @Test void autoWeaponSwitchesWhileAimingNotOnHit() {
        FakePlatform p = new FakePlatform();
        AioCore core = newCore(p);
        AutoWeaponModule m = core.module(AutoWeaponModule.class);
        m.setEnabled(true);
        ((dev.aio.core.setting.BooleanSetting) m.setting("restore")).set(true);
        p.selected = 0;
        p.bestWeapon = 3;
        core.bus().post(new Events.AttackEntity(true));
        assertEquals(0, p.selected, "kein Wechsel im Angriffs-Tick (Attribute Swapping)");
        p.onLiving = true;
        core.bus().post(Events.Tick.INSTANCE);
        assertEquals(3, p.selected, "Wechsel schon beim Anvisieren");
        p.onLiving = false;
        for (int i = 0; i < 25; i++) core.bus().post(Events.Tick.INSTANCE);
        assertEquals(0, p.selected, "danach zurück");
    }

    @Test void sortMergesAndOrdersChest() {
        FakeContainer c = new FakeContainer(27);
        c.set(0, "minecraft:stone", 10);
        c.set(3, "minecraft:dirt", 5);
        c.set(5, "minecraft:stone", 60);
        c.set(9, "minecraft:apple", 3);
        c.set(12, "minecraft:dirt", 7);
        c.set(30, "minecraft:diamond", 1); // Spielerinventar bleibt unberührt
        assertTrue(InventoryOps.sort(c, InventoryOps.sameRegion(c, c.slots().get(0))));
        assertEquals("minecraft:apple", c.get(0).id());
        assertEquals("minecraft:dirt", c.get(1).id());
        assertEquals(12, c.get(1).count());
        assertEquals("minecraft:stone", c.get(2).id());
        assertEquals(64, c.get(2).count());
        assertEquals("minecraft:stone", c.get(3).id());
        assertEquals(6, c.get(3).count());
        assertTrue(c.get(4).isEmpty());
        assertEquals(70, c.total("minecraft:stone"), "nichts geht verloren");
        assertEquals(12, c.total("minecraft:dirt"));
        assertEquals("minecraft:diamond", c.get(30).id());
        assertTrue(c.cursor().isEmpty());
    }

    @Test void sortPlayerMainKeepsHotbar() {
        FakeContainer c = new FakeContainer(0);
        c.set(0, "b", 1);   // Hauptinventar
        c.set(1, "a", 1);
        c.set(27, "z", 1);  // Hotbar
        InventoryTweaksModule mod = new InventoryTweaksModule();
        AioCore core = newCore(new FakePlatform());
        InventoryTweaksModule m = core.module(InventoryTweaksModule.class);
        assertTrue(m.onMiddleClick(c, 1, false));
        assertEquals("a", c.get(0).id());
        assertEquals("b", c.get(1).id());
        assertEquals("z", c.get(27).id(), "Hotbar unverändert");
        assertNotNull(mod);
    }

    @Test void wheelMovesSingleItems() {
        FakeContainer c = new FakeContainer(27);
        c.set(0, "minecraft:iron_ingot", 10);
        assertTrue(InventoryOps.pushOne(c, 0));
        assertEquals(9, c.get(0).count());
        int target = -1;
        for (int i = 27; i < 63; i++) if (c.get(i).id().equals("minecraft:iron_ingot")) target = i;
        assertTrue(target >= 27);
        assertEquals(1, c.get(target).count());
        assertTrue(InventoryOps.pushOne(c, 0));
        assertEquals(2, c.get(target).count(), "stapelt auf denselben Stack");
        assertTrue(InventoryOps.pullOne(c, 0));
        assertEquals(9, c.get(0).count());
        assertEquals(10, c.total("minecraft:iron_ingot"));
        assertTrue(c.cursor().isEmpty());
    }

    @Test void shiftDragQuickMovesEachSlotOnce() {
        FakePlatform p = new FakePlatform();
        AioCore core = newCore(p);
        InventoryTweaksModule m = core.module(InventoryTweaksModule.class);
        FakeContainer c = new FakeContainer(27);
        c.set(0, "x", 5);
        c.set(1, "y", 5);
        m.onDragStart(-1);
        assertTrue(m.onDrag(c, 0, true));
        assertFalse(m.onDrag(c, 0, true), "nicht doppelt");
        assertTrue(m.onDrag(c, 1, true));
        assertTrue(c.get(0).isEmpty() && c.get(1).isEmpty());
        assertEquals(5, c.total("x"));
    }

    @Test void itemSearch() {
        ItemRef a = item("minecraft:diamond_sword", "Diamantschwert", "Minecraft");
        ItemRef b = item("create:wrench", "Schraubenschlüssel", "Create");
        ItemRef c = item("minecraft:diamond", "Diamant", "Minecraft");
        List<ItemRef> all = List.of(a, b, c);
        assertEquals(List.of(a, c), ItemSearch.filter(all, "diamant"));
        assertEquals(List.of(b), ItemSearch.filter(all, "@create"));
        assertEquals(List.of(a, c), ItemSearch.filter(all, "@minecraft"));
        assertEquals(List.of(c), ItemSearch.filter(all, "diamant -schwert"));
        assertEquals(List.of(a), ItemSearch.filter(all, "$sword"));
        assertEquals(all, ItemSearch.filter(all, ""));
    }

    private static ItemRef item(String id, String name, String mod) {
        return new ItemRef() {
            @Override public boolean isEmpty() { return false; }
            @Override public int count() { return 1; }
            @Override public int damage() { return 0; }
            @Override public int maxDamage() { return 0; }
            @Override public String name() { return name; }
            @Override public String id() { return id; }
            @Override public String modName() { return mod; }
        };
    }

    @Test void newHudElementsRender() {
        FakePlatform p = new FakePlatform();
        p.singleplayer = false;
        p.heldDetails = List.of("Sharpness V");
        p.mainHand = item("minecraft:diamond_sword", "Schwert", "Minecraft");
        p.effects.add(new dev.aio.core.platform.EffectInfo("Tempo", 0, 200, false, true, "minecraft:mob_effect/speed"));
        AioCore core = newCore(p);
        for (Module m : core.modules().all()) m.setEnabled(true);
        FakeRenderer r = new FakeRenderer();
        core.bus().post(Events.Tick.INSTANCE);
        core.bus().post(new Events.RenderHud(r, 0));
        assertEquals(0, r.depth);
        assertTrue(r.spriteCalls > 0, "Effekt-Icons werden gezeichnet");
        for (var el : core.hud().elements()) {
            el.render(r, true);
            assertTrue(el.width(r) > 0, el.id());
        }
        assertEquals(0, r.depth);
    }
}
