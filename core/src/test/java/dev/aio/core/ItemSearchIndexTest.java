package dev.aio.core;

import dev.aio.core.modules.inventory.ItemSearch;
import dev.aio.core.platform.ItemRef;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ItemSearchIndexTest {
    private record It(String id, String name, String modName) implements ItemRef {
        @Override public boolean isEmpty() { return false; }
        @Override public int count() { return 1; }
        @Override public int damage() { return 0; }
        @Override public int maxDamage() { return 0; }
    }

    private final List<ItemRef> items = List.of(new It("minecraft:diamond", "Diamant", "Minecraft"),
            new It("minecraft:diamond_sword", "Diamond Sword", "Minecraft"), new It("create:cog", "Zahnrad", "Create"));

    @Test void indexMatchesLikeFilterWhileTypingAndDeleting() {
        ItemSearch.Index idx = new ItemSearch.Index(items);
        for (String q : new String[]{"d", "di", "diamants", "diamant", "", "@create", "-schwert", "dia -schwert", "$sword"}) {
            assertEquals(ItemSearch.filter(items, q), idx.filter(q), q);
        }
    }
}
