package dev.aio.core;

import dev.aio.core.modules.hud.ArmorHudModule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Armor HUD ganz links: ohne Hintergrund liegen die Icons direkt am Rand. */
class ArmorEdgeTest {
    @TempDir Path dir;

    @Test void armorSitsFlushAgainstTheLeftEdge() {
        AioCore core = new AioCore(new FakePlatform(), dir);
        core.registerDefaultModules();
        core.start();
        ArmorHudModule armor = core.module(ArmorHudModule.class);
        FakeRenderer r = new FakeRenderer();
        armor.setScreenPos(r, 0, 100);
        // Innenabstand (4 px) ragt bis auf 1 px über den Rand, die Icons beginnen bei x = 1
        assertEquals(-3f, armor.screenX(r), 0.01f);
    }
}
