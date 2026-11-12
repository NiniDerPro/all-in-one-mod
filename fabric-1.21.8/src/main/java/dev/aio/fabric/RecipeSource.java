package dev.aio.fabric;

import dev.aio.core.platform.ItemRef;
import dev.aio.core.platform.RecipeView;

import net.minecraft.client.Minecraft;

import java.util.List;

/** Recipes for the item list: not available on this Minecraft version yet (the recipe API differs a lot). */
final class RecipeSource {
    private RecipeSource() {}

    static void clear() {}

    static boolean available(Minecraft client) { return false; }

    static List<RecipeView> find(Minecraft client, ItemRef item, boolean usages) { return List.of(); }
}
