package dev.aio.core;

import dev.aio.core.modules.client.KeyBindIndex;
import dev.aio.core.modules.player.ElytraSwapModule;
import dev.aio.core.platform.KeyBinding;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KeyBindIndexTest {
    @TempDir Path dir;

    private final List<KeyBinding> vanilla = new ArrayList<>(List.of(
            new KeyBinding("key.forward", "Vorwärts", "Bewegung", "key.keyboard.w", "key.keyboard.w"),
            new KeyBinding("key.jump", "Springen", "Bewegung", "key.keyboard.space", "key.keyboard.space"),
            new KeyBinding("key.drop", "Fallen lassen", "Gameplay", "key.keyboard.w", "key.keyboard.q"),
            new KeyBinding("key.chat", "Chat", "Mehrspieler", "key.keyboard.unknown", "key.keyboard.t")));

    private AioCore core() {
        FakePlatform p = new FakePlatform() {
            @Override public List<KeyBinding> keyBindings() { return vanilla; }
            @Override public void setKeyBinding(String id, String key) {
                for (int i = 0; i < vanilla.size(); i++) {
                    KeyBinding b = vanilla.get(i);
                    if (b.id().equals(id)) vanilla.set(i, new KeyBinding(b.id(), b.name(), b.category(), key, b.defaultKey()));
                }
            }
        };
        AioCore core = new AioCore(p, dir);
        core.registerDefaultModules();
        core.start();
        return core;
    }

    @Test void findsConflictsUnboundAndSearches() {
        KeyBindIndex idx = new KeyBindIndex(core());
        idx.refresh();
        List<KeyBindIndex.Entry> conflicts = idx.entries("", KeyBindIndex.Filter.CONFLICTS, KeyBindIndex.Sort.DEFAULT);
        assertEquals(List.of("key.forward", "key.drop"), conflicts.stream().map(e -> e.binding().id()).toList());
        assertEquals("key.chat", idx.entries("", KeyBindIndex.Filter.UNBOUND, KeyBindIndex.Sort.DEFAULT).get(0).binding().id());
        assertEquals(1, idx.entries("spring", KeyBindIndex.Filter.ALL, KeyBindIndex.Sort.DEFAULT).size());
        assertTrue(idx.entries("", KeyBindIndex.Filter.ALL, KeyBindIndex.Sort.DEFAULT).stream()
                .anyMatch(e -> e.binding().id().equals("aio:elytraswap.key")), "AIO-Tasten sind dabei");
        assertFalse(idx.freeKeys().contains("key.keyboard.w"));
        assertTrue(idx.freeKeys().contains("key.keyboard.z"));
    }

    @Test void setsVanillaAndAioKeysAndResetsAll() {
        AioCore core = core();
        KeyBindIndex idx = new KeyBindIndex(core);
        idx.refresh();
        KeyBinding drop = idx.bindings().stream().filter(b -> b.id().equals("key.drop")).findFirst().orElseThrow();
        idx.set(drop, "key.keyboard.g");
        assertTrue(idx.entries("", KeyBindIndex.Filter.CONFLICTS, KeyBindIndex.Sort.DEFAULT).isEmpty());
        KeyBinding elytra = idx.bindings().stream().filter(b -> b.id().equals("aio:elytraswap.key")).findFirst().orElseThrow();
        idx.set(elytra, "key.keyboard.h");
        assertEquals("key.keyboard.h", core.module(ElytraSwapModule.class).setting("key").get());
        idx.resetAll();
        assertEquals("key.keyboard.q", vanilla.get(2).key());
        assertEquals("key.keyboard.t", vanilla.get(3).key());
        assertEquals("key.keyboard.grave.accent", core.module(ElytraSwapModule.class).setting("key").get());
    }
}
