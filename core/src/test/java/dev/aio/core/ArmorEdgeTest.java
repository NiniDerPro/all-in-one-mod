package dev.aio.core;

import dev.aio.core.modules.hud.ArmorHudModule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Armor HUD at the far left: without background the icons sit directly at the edge. */
class ArmorEdgeTest {
    @TempDir Path dir;

    @Test void armorSitsFlushAgainstTheLeftEdge() {
        AioCore core = new AioCore(new FakePlatform(), dir);
        core.registerDefaultModules();
        core.start();
        ArmorHudModule armor = core.module(ArmorHudModule.class);
        FakeRenderer r = new FakeRenderer();
        armor.setScreenPos(r, 0, 100);
        // Padding (4 px) extends up to 1 px beyond the edge, the icons start at x = 1
        assertEquals(-3f, armor.screenX(r), 0.01f);
    }
}
