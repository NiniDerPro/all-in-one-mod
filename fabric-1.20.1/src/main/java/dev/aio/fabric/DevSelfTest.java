package dev.aio.fabric;

import dev.aio.core.AioCore;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;

import java.io.File;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Development only (start with {@code -Daio.selftest=<folder>}): opens the most important
 * screens one after another in the title menu and in a test world, takes a real
 * screenshot of each and quits the client. Without the property nothing happens.
 */
final class DevSelfTest {
    private DevSelfTest() {}

    private static final String DIR = System.getProperty("aio.selftest", "");
    /** With {@code -Daio.selftest.interactive=true}: stay open after loading the world and log changes. */
    private static final boolean INTERACTIVE = Boolean.getBoolean("aio.selftest.interactive");
    private static final java.util.Map<String, String> lastState = new java.util.HashMap<>();

    /** A step: waits until {@code ready} holds, then runs {@code run} after {@code delay} ticks. */
    private record Step(BooleanSupplier ready, int delay, Runnable run) {}

    private static List<Step> steps;
    private static int index, waited, stuck;
    private static net.minecraft.world.phys.Vec3 fcStart;

    static void tick(AioCore core, Minecraft mc) {
        if (DIR.isEmpty()) return;
        if (steps == null) steps = plan(core, mc, new File(DIR));
        if (index >= steps.size()) {
            runPending(mc);
            if (INTERACTIVE) logChanges(core, mc);
            return;
        }
        Step s = steps.get(index);
        if (!s.ready.getAsBoolean()) {
            waited = 0;
            if (++stuck % 200 == 0) {
                AioFabricClient.LOGGER.info("[aio] Selftest waiting at step {}: screen={} overlay={} level={}",
                        index, mc.screen, mc.getOverlay(), mc.level != null);
                shot(mc, new File(DIR), "wait_" + stuck + ".png");
            }
            return;
        }
        stuck = 0;
        if (++waited < s.delay) return;
        waited = 0;
        index++;
        try {
            s.run.run();
        } catch (RuntimeException e) {
            AioFabricClient.LOGGER.error("[aio] Selftest step {} failed", index, e);
        }
    }

    private static List<Step> plan(AioCore core, Minecraft mc, File dir) {
        dir.mkdirs();
        BooleanSupplier booted = () -> mc.getOverlay() == null && mc.screen != null;
        BooleanSupplier any = () -> true;
        BooleanSupplier inWorld = () -> mc.level != null && mc.player != null && mc.screen == null && mc.getOverlay() == null;
        return List.of(
                // On the first start the accessibility onboarding comes first: go straight to the title.
                new Step(booted, 40, () -> { if (!(mc.screen instanceof TitleScreen)) mc.setScreen(new TitleScreen()); }),
                new Step(any, 40, () -> shot(mc, dir, "01_title.png")),
                new Step(any, 5, () -> core.openClickGui(null)),
                new Step(any, 30, () -> shot(mc, dir, "02_menu.png")),
                new Step(any, 5, () -> core.openClickGui(core.modules().get("waypoints"))),
                new Step(any, 30, () -> shot(mc, dir, "03_module_settings.png")),
                new Step(any, 5, () -> core.openClickGui(core.clientSettings())),
                new Step(any, 30, () -> shot(mc, dir, "04_client_settings.png")),
                new Step(any, 5, () -> CreateWorldScreen.openFresh(mc, new TitleScreen())),
                new Step(() -> mc.screen instanceof CreateWorldScreen, 40, () -> pressCreate(mc)),
                new Step(inWorld, 200, () -> shot(mc, dir, "05_world_hud.png")),
                new Step(any, 5, () -> {
                    if (!INTERACTIVE) return;
                    logChanges(core, mc);
                    runCommands(mc);
                    AioFabricClient.LOGGER.info("[aio] Selftest ready for input");
                    index = Integer.MAX_VALUE; // skip the rest, input comes from outside
                }),
                new Step(any, 5, () -> { Freecam.set(true); fcStart = Freecam.camera() == null ? null : Freecam.camera().position(); mc.options.keyUp.setDown(true); }),
                new Step(any, 60, () -> {
                    var cam = Freecam.camera();
                    AioFabricClient.LOGGER.info("[aio] Selftest freecam: active={} start={} now={} moved={}", Freecam.active(), fcStart,
                            cam == null ? null : cam.position(), cam != null && fcStart != null && cam.position().distanceTo(fcStart) > 0.5);
                    mc.options.keyUp.setDown(false);
                    Freecam.set(false);
                }),
                new Step(any, 5, () -> mc.pauseGame(false)),
                new Step(any, 30, () -> shot(mc, dir, "06_pause.png")),
                new Step(any, 5, () -> core.openClickGui(null)),
                new Step(any, 30, () -> shot(mc, dir, "07_menu_ingame.png")),
                new Step(any, 5, () -> core.openHudEditor()),
                new Step(any, 30, () -> shot(mc, dir, "08_hud_editor.png")),
                new Step(any, 20, () -> {
                    AioFabricClient.LOGGER.info("[aio] Selftest done: {}", dir.getAbsolutePath());
                    mc.stop();
                }));
    }

    /** "Create" in the create-world screen (private method, hence via reflection). */
    private static void pressCreate(Minecraft mc) {
        try {
            var m = CreateWorldScreen.class.getDeclaredMethod("onCreate");
            m.setAccessible(true);
            m.invoke(mc.screen);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** {@code -Daio.selftest.cmds=a;b;c}: run commands as server operator (e.g. mobs for nametags). */
    private static void runCommands(Minecraft mc) {
        String raw = System.getProperty("aio.selftest.cmds", "");
        if (raw.startsWith("@")) { // @file: one command per line (for commands with quotation marks)
            try {
                raw = String.join(";", java.nio.file.Files.readAllLines(java.nio.file.Path.of(raw.substring(1))));
            } catch (java.io.IOException e) {
                AioFabricClient.LOGGER.error("[aio] Selftest command file", e);
                return;
            }
        }
        // "wait" separates stages; every further stage runs 5 s later (e.g. after a dimension change)
        String[] stages = raw.split(";\\s*wait\\s*(;|$)");
        for (int i = 0; i < stages.length; i++) PENDING.add(new Stage(i * 100, stages[i]));
        stageTicks = 0;
    }

    private record Stage(int atTick, String commands) {}

    private static final java.util.List<Stage> PENDING = new java.util.ArrayList<>();
    private static int stageTicks;

    private static void runPending(Minecraft mc) {
        if (PENDING.isEmpty()) return;
        stageTicks++;
        var server = mc.getSingleplayerServer();
        if (server == null) return;
        while (!PENDING.isEmpty() && PENDING.get(0).atTick() <= stageTicks) {
            String cmds = PENDING.remove(0).commands();
            server.executeIfPossible(() -> {
                for (String c : cmds.split(";")) {
                    if (c.isBlank()) continue;
                    if (c.strip().equals("lan")) { // open the world to LAN (port 25565), for a second client
                        server.setUsesAuthentication(false); // offline: second test client without Mojang login
                        boolean ok = server.publishServer(null, true, 25565);
                        AioFabricClient.LOGGER.info("[aio] Selftest LAN published: {}", ok);
                        continue;
                    }
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c.strip());
                    AioFabricClient.LOGGER.info("[aio] Selftest command: {}", c.strip());
                }
            });
        }
    }

    /** Writes every change to modules, settings, profile and open screen to the log. */
    private static void logChanges(AioCore core, Minecraft mc) {
        java.util.Map<String, String> now = new java.util.LinkedHashMap<>();
        now.put("screen", mc.screen == null ? "none" : mc.screen instanceof AioScreen a
                ? a.ui().getClass().getSimpleName() : mc.screen.getClass().getSimpleName());
        now.put("profile", core.config().activeProfile());
        now.put("sprinting", String.valueOf(mc.player != null && mc.player.isSprinting()));
        now.put("profiles", String.valueOf(core.config().profiles()));
        for (var m : core.modules().all()) {
            now.put(m.id(), m.isEnabled() ? "on" : "off");
            for (var st : m.settings()) now.put(m.id() + "." + st.id(), String.valueOf(st.get()));
        }
        for (var e : now.entrySet()) {
            String old = lastState.put(e.getKey(), e.getValue());
            if (old != null && !old.equals(e.getValue())) {
                AioFabricClient.LOGGER.info("[aio] Selftest change {}: {} -> {}", e.getKey(), old, e.getValue());
            }
        }
    }

    private static void shot(Minecraft mc, File dir, String name) {
        Screenshot.grab(dir, name, mc.getMainRenderTarget(),
                msg -> AioFabricClient.LOGGER.info("[aio] Selftest screenshot {}: {}", name, msg.getString()));
    }
}
