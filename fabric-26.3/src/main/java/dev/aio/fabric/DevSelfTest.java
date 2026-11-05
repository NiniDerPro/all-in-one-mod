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
 * Nur für die Entwicklung (Start mit {@code -Daio.selftest=<Ordner>}): öffnet nacheinander die
 * wichtigsten Oberflächen im Titelmenü und in einer Testwelt, macht von jeder einen echten
 * Screenshot und beendet den Client. Ohne die Eigenschaft passiert nichts.
 */
final class DevSelfTest {
    private DevSelfTest() {}

    private static final String DIR = System.getProperty("aio.selftest", "");
    /** Mit {@code -Daio.selftest.interactive=true}: nach dem Laden der Welt offen bleiben und Änderungen protokollieren. */
    private static final boolean INTERACTIVE = Boolean.getBoolean("aio.selftest.interactive");
    private static final java.util.Map<String, String> lastState = new java.util.HashMap<>();

    /** Ein Schritt: wartet, bis {@code ready} gilt, und führt dann nach {@code delay} Ticks {@code run} aus. */
    private record Step(BooleanSupplier ready, int delay, Runnable run) {}

    private static List<Step> steps;
    private static int index, waited, stuck;

    static void tick(AioCore core, Minecraft mc) {
        if (DIR.isEmpty()) return;
        if (steps == null) steps = plan(core, mc, new File(DIR));
        if (index >= steps.size()) {
            if (INTERACTIVE) logChanges(core, mc);
            return;
        }
        Step s = steps.get(index);
        if (!s.ready.getAsBoolean()) {
            waited = 0;
            if (++stuck % 200 == 0) {
                AioFabricClient.LOGGER.info("[aio] Selftest waiting at step {}: screen={} overlay={} level={}",
                        index, mc.gui.screen(), mc.gui.overlay(), mc.level != null);
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
        BooleanSupplier booted = () -> mc.gui.overlay() == null && mc.gui.screen() != null;
        BooleanSupplier any = () -> true;
        BooleanSupplier inWorld = () -> mc.level != null && mc.player != null && mc.gui.screen() == null && mc.gui.overlay() == null;
        return List.of(
                // Beim ersten Start kommt erst das Accessibility-Onboarding: direkt zum Titel.
                new Step(booted, 40, () -> { if (!(mc.gui.screen() instanceof TitleScreen)) mc.gui.setScreen(new TitleScreen()); }),
                new Step(any, 40, () -> shot(mc, dir, "01_title.png")),
                new Step(any, 5, () -> core.openClickGui(null)),
                new Step(any, 30, () -> shot(mc, dir, "02_menu.png")),
                new Step(any, 5, () -> core.openClickGui(core.modules().get("waypoints"))),
                new Step(any, 30, () -> shot(mc, dir, "03_module_settings.png")),
                new Step(any, 5, () -> core.openClickGui(core.clientSettings())),
                new Step(any, 30, () -> shot(mc, dir, "04_client_settings.png")),
                new Step(any, 5, () -> CreateWorldScreen.openFresh(mc, () -> mc.gui.setScreen(new TitleScreen()))),
                new Step(() -> mc.gui.screen() instanceof CreateWorldScreen, 40, () -> pressCreate(mc)),
                new Step(inWorld, 200, () -> shot(mc, dir, "05_world_hud.png")),
                new Step(any, 5, () -> {
                    if (!INTERACTIVE) return;
                    logChanges(core, mc);
                    runCommands(mc);
                    AioFabricClient.LOGGER.info("[aio] Selftest ready for input");
                    index = Integer.MAX_VALUE; // Rest überspringen, Eingaben kommen von außen
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

    /** "Create" im Welt-Erstellen-Bildschirm (private Methode, daher per Reflection). */
    private static void pressCreate(Minecraft mc) {
        try {
            var m = CreateWorldScreen.class.getDeclaredMethod("onCreate");
            m.setAccessible(true);
            m.invoke(mc.gui.screen());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** {@code -Daio.selftest.cmds=a;b;c}: Befehle als Server-Operator ausführen (z. B. Mobs für Nametags). */
    private static void runCommands(Minecraft mc) {
        String raw = System.getProperty("aio.selftest.cmds", "");
        if (raw.startsWith("@")) { // @Datei: ein Befehl pro Zeile (für Befehle mit Anführungszeichen)
            try {
                raw = String.join(";", java.nio.file.Files.readAllLines(java.nio.file.Path.of(raw.substring(1))));
            } catch (java.io.IOException e) {
                AioFabricClient.LOGGER.error("[aio] Selftest command file", e);
                return;
            }
        }
        String cmds = raw;
        var server = mc.getSingleplayerServer();
        if (cmds.isBlank() || server == null) return;
        server.executeIfPossible(() -> {
            for (String c : cmds.split(";")) {
                if (c.isBlank()) continue;
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c.strip());
                AioFabricClient.LOGGER.info("[aio] Selftest command: {}", c.strip());
            }
        });
    }

    /** Schreibt jede Änderung an Modulen, Einstellungen, Profil und offenem Screen ins Log. */
    private static void logChanges(AioCore core, Minecraft mc) {
        java.util.Map<String, String> now = new java.util.LinkedHashMap<>();
        now.put("screen", mc.gui.screen() == null ? "none" : mc.gui.screen() instanceof AioScreen a
                ? a.ui().getClass().getSimpleName() : mc.gui.screen().getClass().getSimpleName());
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
        Screenshot.grab(dir, name, mc.gameRenderer.mainRenderTarget(), 1,
                msg -> AioFabricClient.LOGGER.info("[aio] Selftest screenshot {}: {}", name, msg.getString()));
    }
}
