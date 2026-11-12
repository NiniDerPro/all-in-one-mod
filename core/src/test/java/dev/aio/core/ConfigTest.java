package dev.aio.core;

import dev.aio.core.config.ConfigManager;
import dev.aio.core.hud.HudElement;
import dev.aio.core.modules.hud.FpsModule;
import dev.aio.core.modules.hud.KeystrokesModule;
import dev.aio.core.setting.NumberSetting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {
    @TempDir Path dir;

    private AioCore newCore() {
        AioCore core = new AioCore(new FakePlatform(), dir);
        core.registerDefaultModules();
        core.start();
        return core;
    }

    @Test void savesAndLoadsModuleState() {
        AioCore a = newCore();
        a.module(FpsModule.class).setEnabled(true);
        ((NumberSetting) a.module(FpsModule.class).setting("scale")).set(1.5);
        a.module(KeystrokesModule.class).setEnabled(true);
        a.shutdown();

        assertTrue(Files.isRegularFile(dir.resolve("profiles/default/fps.json")));
        AioCore b = newCore();
        assertTrue(b.module(FpsModule.class).isEnabled());
        assertEquals(1.5, ((NumberSetting) b.module(FpsModule.class).setting("scale")).get());
        assertTrue(b.module(KeystrokesModule.class).isEnabled());
    }

    @Test void corruptFileFallsBackToDefaults() throws Exception {
        Files.createDirectories(dir.resolve("profiles/default"));
        Files.writeString(dir.resolve("profiles/default/fps.json"), "{ kaputt");
        AioCore core = newCore();
        assertFalse(core.module(FpsModule.class).isEnabled());
        assertEquals(1.0, ((NumberSetting) core.module(FpsModule.class).setting("scale")).get());
    }

    @Test void invalidValuesAreClamped() throws Exception {
        Files.createDirectories(dir.resolve("profiles/default"));
        Files.writeString(dir.resolve("profiles/default/fps.json"),
                "{\"enabled\":true,\"settings\":{\"scale\":99,\"textColor\":\"nope\",\"showAverage\":\"x\"}}");
        AioCore core = newCore();
        assertTrue(core.module(FpsModule.class).isEnabled());
        assertEquals(HudElement.MAX_SCALE, ((NumberSetting) core.module(FpsModule.class).setting("scale")).get());
        assertEquals(HudElement.MAX_SCALE, ((NumberSetting) core.module(FpsModule.class).setting("scaleY")).get(), "old size also applies to the height");
        assertEquals(0xFFFFFFFF, core.module(FpsModule.class).setting("textColor").get());
    }

    @Test void saveIsDebounced() {
        long[] now = {0};
        AioCore core = newCore();
        ConfigManager cfg = new ConfigManager(dir.resolve("debounce"), core.modules(), () -> now[0]);
        cfg.markDirty();
        cfg.tick();
        assertTrue(cfg.isDirty());
        now[0] = ConfigManager.SAVE_DELAY_MS;
        cfg.tick();
        assertFalse(cfg.isDirty());
        assertTrue(Files.exists(dir.resolve("debounce/client.json")));
    }

    @Test void profilesSwitchCreateDelete() {
        AioCore core = newCore();
        ConfigManager cfg = core.config();
        core.module(FpsModule.class).setEnabled(true);
        String pvp = cfg.createProfile("pvp");
        assertEquals("pvp", pvp);
        assertTrue(core.module(FpsModule.class).isEnabled(), "new profile is a copy");
        core.module(FpsModule.class).setEnabled(false);

        cfg.switchProfile("default");
        assertTrue(core.module(FpsModule.class).isEnabled());
        cfg.switchProfile("pvp");
        assertFalse(core.module(FpsModule.class).isEnabled());

        assertEquals("pvp-2", cfg.createProfile("pvp"));
        assertTrue(cfg.profiles().containsAll(List.of("default", "pvp", "pvp-2")));
        assertFalse(cfg.deleteProfile("pvp-2"), "active profile cannot be deleted");
        cfg.switchProfile("default");
        assertTrue(cfg.deleteProfile("pvp-2"));
        assertFalse(cfg.profiles().contains("pvp-2"));

        AioCore reloaded = newCore();
        assertEquals("default", reloaded.config().activeProfile());
    }

    @Test void profileNamesCannotEscapeDirectory() {
        AioCore core = newCore();
        assertThrows(IllegalArgumentException.class, () -> core.config().switchProfile("../evil"));
        assertFalse(ConfigManager.isValidProfileName("a/b"));
        assertFalse(ConfigManager.isValidProfileName(""));
        assertTrue(ConfigManager.isValidProfileName("PvP_1-x"));
    }

    @Test void exportImportRoundTrip() {
        AioCore core = newCore();
        core.module(FpsModule.class).setEnabled(true);
        ((NumberSetting) core.module(FpsModule.class).setting("scale")).set(2.0);
        String json = core.config().exportProfile();
        assertTrue(Files.exists(dir.resolve("exports/default.json")));

        core.module(FpsModule.class).setEnabled(false);
        String name = core.config().importProfile(json);
        assertEquals("default-2", name);
        assertEquals(name, core.config().activeProfile());
        assertTrue(core.module(FpsModule.class).isEnabled());
        assertEquals(2.0, ((NumberSetting) core.module(FpsModule.class).setting("scale")).get());

        assertThrows(IllegalArgumentException.class, () -> core.config().importProfile("hallo"));
        assertThrows(IllegalArgumentException.class, () -> core.config().importProfile("{\"foo\":1}"));
        assertThrows(IllegalArgumentException.class, () -> core.config().importProfile(null));
        // Name with dangerous characters is sanitised
        String evil = json.replace("\"name\": \"default\"", "\"name\": \"../../x\"");
        assertEquals("x", core.config().importProfile(evil));
    }
}
