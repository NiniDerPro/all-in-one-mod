package dev.aio.core.modules.hud;

import dev.aio.core.AioCore;
import dev.aio.core.FakePlatform;
import dev.aio.core.FakeRenderer;
import dev.aio.core.platform.ItemRef;
import dev.aio.core.platform.LookTarget;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LookAtModuleTest {
    @TempDir Path dir;

    private static final ItemRef DIRT = new ItemRef() {
        @Override public boolean isEmpty() { return false; }
        @Override public int count() { return 1; }
        @Override public int damage() { return 0; }
        @Override public int maxDamage() { return 0; }
        @Override public String name() { return "Erde"; }
    };

    private AioCore newCore(FakePlatform p) {
        AioCore core = new AioCore(p, dir);
        core.registerDefaultModules();
        core.start();
        return core;
    }

    @Test void enabledByDefaultTopCenterClassicLook() {
        AioCore core = newCore(new FakePlatform());
        LookAtModule m = core.module(LookAtModule.class);
        assertTrue(m.isEnabled());
        assertEquals(0.5, m.setting("x").get());
        assertEquals(0.0, m.setting("y").get());
        assertEquals(LookAtModule.BG, m.setting("backgroundColor").get());
        assertEquals(dev.aio.core.hud.ColorMode.STATIC, m.colorMode());
    }

    @Test void blockShowsToolGrowthAndMod() {
        FakePlatform p = new FakePlatform();
        AioCore core = newCore(p);
        LookAtModule m = core.module(LookAtModule.class);
        p.lookTarget = LookTarget.block("Weizen", "Minecraft", DIRT, false, "Spitzhacke", 0.5f, 1, 2, 3);
        m.update();
        assertEquals("Weizen", m.targetName());
        assertEquals(List.of("✘ Spitzhacke", "Wachstum: 50%", "§oMinecraft"), m.lineTexts());
        assertTrue(m.hasContent());
        assertTrue(m.iconShown());

        p.lookTarget = LookTarget.block("Weizen", "Minecraft", DIRT, true, "Spitzhacke", 1f, 1, 2, 3);
        m.update();
        assertEquals("✔ Spitzhacke", m.lineTexts().get(0));
        assertEquals("Wachstum: ausgewachsen", m.lineTexts().get(1));

        p.lookTarget = LookTarget.block("Eichenstamm", "Minecraft", DIRT, null, "Axt", Float.NaN, 1, 2, 3);
        m.update();
        assertEquals("Axt", m.lineTexts().get(0), "ohne Axt in der Hand neutral");
    }

    @Test void blockDetailsOnlyForThatBlock() {
        FakePlatform p = new FakePlatform();
        AioCore core = newCore(p);
        LookAtModule m = core.module(LookAtModule.class);
        p.lookTarget = LookTarget.block("Bienenstock", "Minecraft", DIRT, null, "Axt", Float.NaN, 0, 0, 0,
                List.of(LookTarget.Detail.of("Honig", 5, 5), LookTarget.Detail.of("Bienen", 2, 3)));
        m.update();
        assertEquals(List.of("Axt", "Honig: 5/5", "Bienen: 2/3", "§oMinecraft"), m.lineTexts());
        assertTrue(LookTarget.Detail.of("Honig", 5, 5).highlight(), "voll = hervorgehoben");
        assertFalse(LookTarget.Detail.of("Bienen", 2, 3).highlight());

        p.lookTarget = LookTarget.block("Stein", "Minecraft", DIRT, null, null, Float.NaN, 0, 0, 0);
        m.update();
        assertEquals(List.of("§oMinecraft"), m.lineTexts(), "andere Blöcke ohne Details");

        ((dev.aio.core.setting.BooleanSetting) m.setting("showDetails")).set(false);
        p.lookTarget = LookTarget.block("Komposter", "Minecraft", DIRT, null, null, Float.NaN, 0, 0, 0,
                List.of(LookTarget.Detail.of("Füllstand", 3, 7)));
        m.update();
        assertEquals(List.of("§oMinecraft"), m.lineTexts(), "Details abschaltbar");
    }

    @Test void entityShowsHeartsOrNumbersAndArmor() {
        FakePlatform p = new FakePlatform();
        AioCore core = newCore(p);
        LookAtModule m = core.module(LookAtModule.class);
        p.lookTarget = LookTarget.entity("Zombie", "Minecraft", ItemRef.EMPTY, 15, 20, 2, 0, 0, 0);
        m.update();
        assertEquals(List.of("<hearts> 15 / 20 HP", "Rüstung: 2", "§oMinecraft"), m.lineTexts());
        assertFalse(m.iconShown(), "kein Icon bei Wesen ohne Item");

        p.lookTarget = LookTarget.entity("Wither", "Minecraft", ItemRef.EMPTY, 250, 300, 4, 0, 0, 0);
        m.update();
        assertEquals("❤ 250 / 300 HP", m.lineTexts().get(0), "viel Leben als Zahl statt Herzen");

        p.lookTarget = LookTarget.entity("Bernd", "Zombie", "Minecraft", ItemRef.EMPTY, 20, 20, 0, 0, 0, 0);
        m.update();
        assertEquals("Typ: Zombie", m.lineTexts().get(0), "Typ bei Namensschild");
    }

    @Test void hiddenWithoutTargetAndInMenus() {
        FakePlatform p = new FakePlatform();
        AioCore core = newCore(p);
        LookAtModule m = core.module(LookAtModule.class);
        m.update();
        assertFalse(m.hasContent());
        p.lookTarget = LookTarget.block("Stein", "Minecraft", DIRT, null, null, Float.NaN, 0, 0, 0);
        m.update();
        assertTrue(m.hasContent());
        assertEquals(List.of("§oMinecraft"), m.lineTexts());
        p.screen = new dev.aio.core.ui.UiScreen() {
            @Override public void render(dev.aio.core.platform.Renderer r, int mx, int my, float d) {}
        };
        assertFalse(m.hasContent());
    }

    @Test void rendersBalancedAndCachesWidth() {
        FakePlatform p = new FakePlatform();
        AioCore core = newCore(p);
        LookAtModule m = core.module(LookAtModule.class);
        FakeRenderer r = new FakeRenderer();
        m.render(r, true); // Vorschau ohne Ziel
        assertEquals(0, r.depth);

        p.lookTarget = LookTarget.entity("Zombie", "Minecraft", ItemRef.EMPTY, 15, 20, 0, 0, 0, 0);
        m.update();
        m.render(r, false);
        int calls = r.widthCalls;
        for (int i = 0; i < 50; i++) {
            m.update();
            m.render(r, false);
        }
        assertEquals(0, r.depth);
        assertTrue(r.widthCalls - calls < 50 * 3, "Breite wird nicht jeden Frame neu gemessen");
    }
}
