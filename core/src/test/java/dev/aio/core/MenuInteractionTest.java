package dev.aio.core;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.Setting;
import dev.aio.core.ui.ClickGuiScreen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Bedienung des Hauptmenüs wie ein Spieler: klicken, Tasten, Panel, Zahnrad. */
class MenuInteractionTest {
    @TempDir Path dir;

    private AioCore core;
    private FakePlatform platform;

    private ClickGuiScreen open() {
        platform = new FakePlatform();
        core = new AioCore(platform, dir);
        core.registerDefaultModules();
        core.start();
        return new ClickGuiScreen(core, null);
    }

    private Map<String, Boolean> states() {
        Map<String, Boolean> out = new HashMap<>();
        for (Module m : core.modules().all()) out.put(m.id(), m.isEnabled());
        return out;
    }

    private static void click(ClickGuiScreen gui, FakeRenderer r, int x, int y) {
        gui.render(r, x, y, 0);
        gui.mouseClicked(x, y, 0);
        gui.render(r, x, y, 0);
    }

    /** Animationen ablaufen lassen (im Spiel etwa 0,2 s). */
    private static void settle(ClickGuiScreen gui, FakeRenderer r) throws InterruptedException {
        for (int i = 0; i < 8; i++) {
            gui.render(r, 400, 150, 0);
            Thread.sleep(40);
        }
    }

    @Test void toggleSwitchInARowTurnsExactlyOneModuleOnOrOff() {
        ClickGuiScreen gui = open();
        FakeRenderer r = new FakeRenderer();
        gui.render(r, 0, 0, 0);
        Map<String, Boolean> before = states();
        // erste Zeile, linke Spalte: Schalter sitzt rechts in der Zeile
        boolean changed = false;
        for (int x = 200; x < 238 && !changed; x += 2) {
            click(gui, r, x, 74);
            Map<String, Boolean> after = states();
            int diff = 0;
            for (String id : before.keySet()) if (!before.get(id).equals(after.get(id))) diff++;
            assertTrue(diff <= 1, "höchstens ein Modul pro Klick");
            changed = diff == 1;
        }
        assertTrue(changed, "Schalter in der Zeile schaltet das Modul um");
    }

    @Test void clickingARowOpensSettingsAndEscapeClosesThemFirst() {
        ClickGuiScreen gui = open();
        FakeRenderer r = new FakeRenderer();
        gui.render(r, 0, 0, 0);
        Map<String, Boolean> before = states();
        click(gui, r, 60, 74); // Name der ersten Zeile, nicht der Schalter
        assertEquals(before, states(), "Klick auf den Namen schaltet nichts um");
        assertTrue(gui.keyPressed("key.keyboard.escape"), "erstes ESC schließt das Panel");
        assertFalse(gui.keyPressed("key.keyboard.escape"), "zweites ESC schließt das Menü");
    }

    @Test void settingsPanelTogglesABooleanSetting() {
        ClickGuiScreen gui = open();
        FakeRenderer r = new FakeRenderer();
        Module waypoints = core.modules().get("waypoints");
        BooleanSetting distance = (BooleanSetting) waypoints.setting("distance");
        ClickGuiScreen panel = new ClickGuiScreen(core, waypoints);
        boolean before = distance.on();
        // im Panel rechts (unterhalb des Schließen-Knopfs) auf die Schalterspalte klicken, bis "Show distance" umspringt
        for (int y = 112; y < 240 && distance.on() == before; y += 2) click(panel, r, 452, y);
        assertNotEquals(before, distance.on(), "Schalter im Einstellungs-Panel wirkt");
    }

    @Test void gearOpensClientSettingsWhereProfilesCanBeCreated() throws InterruptedException {
        ClickGuiScreen gui = open();
        FakeRenderer r = new FakeRenderer();
        int profiles = core.config().profiles().size();
        click(gui, r, 452, 25); // Zahnrad oben rechts
        settle(gui, r); // Panel gleitet herein
        gui.mouseScrolled(400, 150, -20); // im Panel nach unten zu den Profilen
        // von unten nach oben (oberhalb von "Reset all"), damit nichts darüber Zeilen verschiebt
        for (int y = 186; y > 60 && core.config().profiles().size() == profiles; y -= 2) click(gui, r, 250, y);
        assertTrue(core.config().profiles().size() > profiles, "Profil über das Zahnrad-Panel anlegen");
    }

    @Test void clickingEverywhereNeverThrows() {
        ClickGuiScreen gui = open();
        FakeRenderer r = new FakeRenderer();
        for (int pass = 0; pass < 2; pass++) {
            for (int y = 0; y < r.height; y += 7) {
                for (int x = 0; x < r.width; x += 9) {
                    gui.render(r, x, y, 0);
                    assertDoesNotThrow(() -> gui.mouseClicked(0, 0, 1));
                }
            }
        }
        // jede Kategorie, jedes Modul-Panel
        for (Category c : Category.values()) {
            List<Module> mods = core.modules().byCategory(c);
            for (Module m : mods) {
                ClickGuiScreen p = new ClickGuiScreen(core, m);
                for (int y = 30; y < 260; y += 6) {
                    int yy = y;
                    assertDoesNotThrow(() -> click(p, r, 400, yy));
                }
                for (Setting<?> s : m.settings()) assertNotNull(s.name());
            }
        }
    }

    @Test void bindingAKeyDoesNotTypeItIntoTheSearch() throws Exception {
        ClickGuiScreen gui = open();
        FakeRenderer r = new FakeRenderer();
        gui.render(r, 0, 0, 0);
        var zoomKey = (dev.aio.core.setting.KeybindSetting) core.modules().get("zoom").settings().stream()
                .filter(st -> st.id().equals("key")).findFirst().orElseThrow();
        var listening = ClickGuiScreen.class.getDeclaredField("listening");
        listening.setAccessible(true);
        listening.set(gui, zoomKey);
        // So kommt es vom Spiel: erst die Taste, dann das Zeichen dazu
        assertTrue(gui.keyPressed("key.keyboard.v"));
        assertTrue(gui.charTyped("v"));
        assertEquals("key.keyboard.v", zoomKey.get());
        var search = ClickGuiScreen.class.getDeclaredField("search");
        search.setAccessible(true);
        assertEquals("", search.get(gui), "das Zeichen der belegten Taste landet nicht in der Suche");
        // danach tippt man normal weiter
        gui.keyPressed("key.keyboard.z");
        gui.charTyped("z");
        assertEquals("z", search.get(gui));
    }

    @Test void reopensWhereItWasClosedUnlessTurnedOff() {
        ClickGuiScreen gui = open();
        FakeRenderer r = new FakeRenderer();
        gui.render(r, 0, 0, 0);
        Module zoom = core.modules().get("zoom");
        ClickGuiScreen focused = new ClickGuiScreen(core, zoom); // Panel und Kategorie von Zoom
        focused.render(r, 0, 0, 0);
        focused.onClose();

        ClickGuiScreen again = new ClickGuiScreen(core, null);
        again.render(r, 0, 0, 0);
        assertTrue(again.keyPressed("key.keyboard.escape"), "Zoom-Panel ist wieder offen und ESC schließt es");

        core.clientSettings().rememberMenu.set(false);
        ClickGuiScreen fresh = new ClickGuiScreen(core, null);
        fresh.render(r, 0, 0, 0);
        assertFalse(fresh.keyPressed("key.keyboard.escape"), "ohne die Option startet das Menü leer");
    }

    @Test void typingReplacesARememberedSearch() throws Exception {
        ClickGuiScreen gui = open();
        FakeRenderer r = new FakeRenderer();
        gui.render(r, 0, 0, 0);
        gui.keyPressed("key.keyboard.z");
        gui.charTyped("z");
        gui.charTyped("o");
        gui.onClose();
        ClickGuiScreen again = new ClickGuiScreen(core, null);
        again.render(r, 0, 0, 0);
        again.keyPressed("key.keyboard.f");
        again.charTyped("f");
        var search = ClickGuiScreen.class.getDeclaredField("search");
        search.setAccessible(true);
        assertEquals("f", search.get(again), "die gemerkte Suche wird beim Lostippen ersetzt");
    }
}
