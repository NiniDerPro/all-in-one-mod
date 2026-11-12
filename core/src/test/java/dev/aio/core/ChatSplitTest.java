package dev.aio.core;

import dev.aio.core.modules.chat.ChatHeadsModule;
import dev.aio.core.modules.chat.ChatModule;
import dev.aio.core.modules.chat.FlexChatModule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Chat split: Custom Chat (off), Flex Chat (on), Chat Heads (off) – each its own module. */
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
        assertFalse(core.module(ChatModule.class).isEnabled(), "Custom Chat off by default");
        assertTrue(core.module(FlexChatModule.class).isEnabled(), "Flex Chat on by default");
        assertFalse(core.module(ChatHeadsModule.class).isEnabled(), "Chat Heads off by default");
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
        assertTrue(again.module(ChatModule.class).isEnabled(), "afterwards the own choice is kept");
    }
}
