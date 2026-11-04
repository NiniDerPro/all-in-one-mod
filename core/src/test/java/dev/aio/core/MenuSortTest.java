package dev.aio.core;

import dev.aio.core.module.Module;
import dev.aio.core.modules.hud.FpsModule;
import dev.aio.core.ui.ClickGuiScreen;
import dev.aio.core.ui.SortMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MenuSortTest {
    @TempDir Path dir;

    private AioCore newCore() {
        AioCore core = new AioCore(new FakePlatform(), dir);
        core.registerDefaultModules();
        core.start();
        return core;
    }

    @Test void alphabeticalAndActiveFirst() {
        AioCore core = newCore();
        List<Module> all = core.modules().all();
        List<Module> az = SortMode.ALPHABETICAL.sort(all);
        for (int i = 1; i < az.size(); i++) {
            assertTrue(az.get(i - 1).name().compareToIgnoreCase(az.get(i).name()) <= 0, "A–Z");
        }
        List<Module> active = SortMode.ACTIVE_FIRST.sort(all);
        boolean seenOff = false;
        for (Module m : active) {
            if (!m.isEnabled()) seenOff = true;
            else assertFalse(seenOff, "aktive zuerst");
        }
        assertEquals(all, SortMode.DEFAULT.sort(all));
    }

    @Test void searchMatchesNameDescriptionAndCategory() {
        AioCore core = newCore();
        List<Module> all = core.modules().all();
        assertTrue(SortMode.search(all, "fps").contains(core.module(FpsModule.class)));
        assertTrue(SortMode.search(all, "kompass").stream().anyMatch(m -> m.id().equals("compass")));
        assertTrue(SortMode.search(all, "inventar").size() >= 3, "nach Kategorie");
        assertTrue(SortMode.search(all, "gibtsnicht").isEmpty());
        assertEquals(all, SortMode.search(all, " "));
    }

    @Test void guiSearchTypingAndSortButtonWork() {
        FakePlatform p = new FakePlatform();
        AioCore core = new AioCore(p, dir);
        core.registerDefaultModules();
        core.start();
        ClickGuiScreen gui = new ClickGuiScreen(core, null);
        FakeRenderer r = new FakeRenderer();
        gui.render(r, 0, 0, 0);
        // Strg+F fokussiert die Suche, Tippen filtert, Taste "e" schließt nichts
        p.heldKeys.add("key.keyboard.left.control");
        assertTrue(gui.keyPressed("key.keyboard.f"));
        p.heldKeys.clear();
        assertTrue(gui.charTyped("z"));
        assertTrue(gui.charTyped("o"));
        assertTrue(gui.keyPressed("key.keyboard.e"), "Tasten gehen ins Suchfeld");
        gui.render(r, 0, 0, 0);
        assertTrue(gui.keyPressed("key.keyboard.escape"));
        assertFalse(gui.keyPressed("key.keyboard.escape"), "zweites Escape schließt das Menü");
        // Sortierknopf durchklicken über die ganze Kopfzeile
        SortMode before = core.clientSettings().sortMode.get();
        for (int x = 130; x < r.width; x += 3) {
            gui.render(r, x, 20, 0);
            gui.mouseClicked(x, 20, 0);
            if (core.clientSettings().sortMode.get() != before) break;
        }
        assertNotEquals(before, core.clientSettings().sortMode.get(), "Sortierknopf schaltet um");
    }
}
