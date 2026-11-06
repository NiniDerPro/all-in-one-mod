package dev.aio.core;

import dev.aio.core.modules.chat.ChatHeadsModule;
import dev.aio.core.modules.chat.ChatModule;
import dev.aio.core.modules.chat.FlexChatModule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Chat aufgeteilt: Custom Chat (aus), Flex Chat (an), Chat Heads (aus) – jeweils eigene Module. */
class ChatSplitTest {
    @TempDir Path dir;

    private AioCore start() {
        AioCore core = new AioCore(new FakePlatform(), dir);
        core.registerDefaultModules();
        core.start();
        return core;
    }

    @Test void freshInstallDefaults() {
        AioCore core = start();
        assertFalse(core.module(ChatModule.class).isEnabled(), "Custom Chat standardmäßig aus");
        assertTrue(core.module(FlexChatModule.class).isEnabled(), "Flex Chat standardmäßig an");
        assertFalse(core.module(ChatHeadsModule.class).isEnabled(), "Chat Heads standardmäßig aus");
    }

    @Test void oldChatSettingIsTurnedOffOnceAndThenRespected() throws Exception {
        Path profile = dir.resolve("profiles").resolve("default");
        Files.createDirectories(profile);
        Files.writeString(profile.resolve("chat.json"), "{\"enabled\": true, \"settings\": {}}");
        AioCore core = start();
        assertFalse(core.module(ChatModule.class).isEnabled(), "altes 'Chat an' wird einmalig ausgeschaltet");
        assertTrue(core.module(FlexChatModule.class).isEnabled());

        core.module(ChatModule.class).setEnabled(true);
        core.config().save();
        core.shutdown();
        AioCore again = start();
        assertTrue(again.module(ChatModule.class).isEnabled(), "danach bleibt die eigene Wahl erhalten");
    }
}
