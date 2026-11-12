package dev.aio.core;

import dev.aio.core.hud.HudElement;
import dev.aio.core.module.Module;
import dev.aio.core.ui.ClickGuiScreen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Rough cost per frame (without GPU): menu and all HUD elements must stay well below 1 ms. */
class RenderCostTest {
    @TempDir Path dir;

    @Test void menuAndHudAreCheapPerFrame() {
        AioCore core = new AioCore(new FakePlatform(), dir);
        core.registerDefaultModules();
        core.start();
        for (Module m : core.modules().all()) if (m instanceof HudElement && m.canToggle()) m.setEnabled(true);
        FakeRenderer r = new FakeRenderer();
        ClickGuiScreen gui = new ClickGuiScreen(core, core.modules().get("zoom"));
        for (int i = 0; i < 2000; i++) gui.render(r, 200, 120, 0); // warm up (JIT)
        long t0 = System.nanoTime();
        int frames = 5000;
        for (int i = 0; i < frames; i++) gui.render(r, 200 + i % 50, 120, 0);
        double menuMs = (System.nanoTime() - t0) / 1e6 / frames;

        for (int i = 0; i < 2000; i++) core.bus().post(new dev.aio.core.event.Events.RenderHud(r, 0));
        t0 = System.nanoTime();
        for (int i = 0; i < frames; i++) core.bus().post(new dev.aio.core.event.Events.RenderHud(r, 0));
        double hudMs = (System.nanoTime() - t0) / 1e6 / frames;
        System.out.printf("[cost] menu %.4f ms/frame, all HUD elements %.4f ms/frame%n", menuMs, hudMs);
        assertTrue(menuMs < 1.0, "Menu " + menuMs + " ms");
        assertTrue(hudMs < 1.0, "HUD " + hudMs + " ms");
    }
}
