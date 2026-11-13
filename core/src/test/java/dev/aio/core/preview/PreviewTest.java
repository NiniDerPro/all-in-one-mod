package dev.aio.core.preview;

import dev.aio.core.AioCore;
import dev.aio.core.FakePlatform;
import dev.aio.core.hud.HudElement;
import dev.aio.core.hud.HudEditorScreen;
import dev.aio.core.module.Module;
import dev.aio.core.platform.ItemRef;
import dev.aio.core.ui.ClickGuiScreen;
import dev.aio.core.ui.UiScreen;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

/**
 * Design preview: renders menu and HUD as PNG. Only runs with
 * {@code -Daio.preview=<Ordner>}, z. B. {@code ./gradlew :core:test -Paio.coreOnly=true -Daio.preview=/tmp/p --tests '*PreviewTest*'}.
 */
class PreviewTest {
    @TempDir Path dir;

    static final File CLIENT_JAR = new File(System.getProperty("aio.clientJar",
            System.getProperty("user.home") + "/.gradle/caches/fabric-loom/26.3/minecraft-client.jar"));

    /** Delivers items with an id so that icons appear in the preview. */
    static final class PreviewPlatform extends FakePlatform {
        @Override public ItemRef item(String id) { return new SimpleItem(id, 1); }
    }

    record SimpleItem(String id, int count) implements ItemRef {
        @Override public boolean isEmpty() { return false; }
        @Override public int damage() { return 0; }
        @Override public int maxDamage() { return 0; }
        @Override public String name() { return id; }
    }

    private AioCore core() {
        AioCore core = new AioCore(new PreviewPlatform(), dir);
        core.registerDefaultModules();
        core.start();
        return core;
    }

    private static void render(UiScreen screen, File out, int mx, int my, boolean world) throws Exception {
        render(screen, out, mx, my, 640, 360);
    }

    private static void render(UiScreen screen, File out, int mx, int my, int w, int h) throws Exception {
        screen.onOpen();
        PreviewRenderer r = null;
        for (int frame = 0; frame < 3; frame++) { // let animations settle, only the last frame counts
            r = new PreviewRenderer(w, h, 2, CLIENT_JAR);
            r.fakeWorld();
            screen.render(r, mx, my, 0);
            Thread.sleep(150);
        }
        r.save(out);
    }

    @Test void renderPreviews() throws Exception {
        String target = System.getProperty("aio.preview", "");
        Assumptions.assumeTrue(!target.isEmpty(), "only with -Daio.preview");
        File outDir = new File(target);
        AioCore core = core();

        ClickGuiScreen gui = new ClickGuiScreen(core, null);
        render(gui, new File(outDir, "menu_all.png"), 300, 120, true);
        render(new ClickGuiScreen(core, null), new File(outDir, "menu_hover.png"), 250, 106, true);

        Module waypoints = core.modules().get("waypoints");
        render(new ClickGuiScreen(core, waypoints), new File(outDir, "menu_settings.png"), 400, 150, true);

        ClickGuiScreen client = new ClickGuiScreen(core, core.clientSettings());
        render(client, new File(outDir, "menu_client.png"), 400, 150, true);
        render(new ClickGuiScreen(core, null), new File(outDir, "menu_small.png"), 200, 100, 427, 240);

        var wpModule = (dev.aio.core.modules.navigation.WaypointsModule) core.modules().get("waypoints");
        wpModule.setEnabled(true);
        wpModule.add(new dev.aio.core.modules.navigation.Waypoint("Home", 120, 64, -340, "sp:Testwelt", "overworld", 0x55FF55, true, false));
        wpModule.add(new dev.aio.core.modules.navigation.Waypoint("Mine", 80, 12, -20, "sp:Testwelt", "overworld", 0xFFAA00, true, false));
        render(new dev.aio.core.modules.navigation.WaypointScreen(core, wpModule), new File(outDir, "waypoints.png"), 300, 120, true);
        render(new dev.aio.core.modules.navigation.WaypointEditScreen(core, wpModule, null), new File(outDir, "waypoint_new.png"), 300, 120, true);
        var friends = (dev.aio.core.modules.render.FriendsModule) core.modules().get("friends");
        friends.setEnabled(true);
        friends.addRule(new dev.aio.core.modules.render.FriendsModule.Rule(dev.aio.core.modules.render.FriendsModule.Match.NAME, "Alex", dev.aio.core.modules.render.FriendsModule.FRIEND_GREEN, true));
        friends.addRule(new dev.aio.core.modules.render.FriendsModule.Rule(dev.aio.core.modules.render.FriendsModule.Match.CONTAINS, "{Team}", 0xFF5599FF, true));
        friends.addRule(new dev.aio.core.modules.render.FriendsModule.Rule(dev.aio.core.modules.render.FriendsModule.Match.STARTS, "Builder_", 0xFFFFAA00, false));
        render(new dev.aio.core.modules.render.FriendsScreen(core, friends), new File(outDir, "friends.png"), 300, 120, true);
        var xhair = (dev.aio.core.modules.render.CrosshairModule) core.modules().get("crosshair");
        xhair.setEnabled(true);
        render(new dev.aio.core.modules.render.CrosshairScreen(core, xhair), new File(outDir, "crosshair_presets.png"), 300, 120, true);
        render(new dev.aio.core.modules.client.KeyBindsScreen(core), new File(outDir, "keybinds.png"), 300, 120, true);
        var particles = (dev.aio.core.modules.performance.ParticleLimitModule) core.modules().get("particlelimit");
        render(new dev.aio.core.modules.performance.ParticleScreen(core, particles), new File(outDir, "particles.png"), 300, 120, true);

        // HUD in the game
        for (String id : new String[]{"fps", "cps", "coordinates", "armorhud", "keystrokes", "potions", "datetime", "compass"}) {
            Module m = core.modules().get(id);
            if (m != null) m.setEnabled(true);
        }
        PreviewRenderer hud = new PreviewRenderer(640, 360, 2, CLIENT_JAR);
        hud.fakeWorld();
        core.hud().placeNew(hud);
        for (HudElement el : core.hud().elements()) if (el.isActive() && el.editable()) el.render(hud, true);
        hud.save(new File(outDir, "hud.png"));

        render(new HudEditorScreen(core), new File(outDir, "hud_editor.png"), 320, 180, true);
    }
}
