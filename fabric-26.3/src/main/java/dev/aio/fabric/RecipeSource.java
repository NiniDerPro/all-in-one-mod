package dev.aio.fabric;

import dev.aio.core.platform.ItemRef;
import dev.aio.core.platform.RecipeView;
import net.fabricmc.fabric.api.recipe.v1.sync.SynchronizedRecipes;
import net.minecraft.client.Minecraft;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.item.crafting.display.StonecutterRecipeDisplay;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Rezepte für die Item-Liste. Quelle: im Singleplayer der integrierte Server, sonst vom
 * Server per Fabric synchronisierte Rezepte (nur wenn der Server das anbietet).
 */
final class RecipeSource {
    private static final int LIMIT = 200;
    private static volatile SynchronizedRecipes synced;

    private RecipeSource() {}

    static void onSynced(SynchronizedRecipes recipes) { synced = recipes; }

    static void clear() { synced = null; }

    static boolean available(Minecraft client) {
        return client.getSingleplayerServer() != null || synced != null;
    }

    private static Collection<RecipeHolder<?>> all(Minecraft client) {
        var server = client.getSingleplayerServer();
        if (server != null) return server.getRecipeManager().recipeMap().values();
        SynchronizedRecipes s = synced;
        if (s == null) return List.of();
        List<RecipeHolder<?>> out = new ArrayList<>();
        out.addAll(s.getAllOfType(RecipeType.CRAFTING));
        out.addAll(s.getAllOfType(RecipeType.SMELTING));
        out.addAll(s.getAllOfType(RecipeType.BLASTING));
        out.addAll(s.getAllOfType(RecipeType.SMOKING));
        out.addAll(s.getAllOfType(RecipeType.CAMPFIRE_COOKING));
        out.addAll(s.getAllOfType(RecipeType.STONECUTTING));
        return out;
    }

    static List<RecipeView> find(Minecraft client, ItemRef item, boolean usages) {
        if (client.level == null || !(item instanceof FabricItemRef target)) return List.of();
        ContextMap ctx = SlotDisplayContext.fromLevel(client.level);
        List<RecipeView> out = new ArrayList<>();
        for (RecipeHolder<?> holder : all(client)) {
            for (RecipeDisplay display : holder.value().display()) {
                RecipeView view = convert(display, ctx);
                if (view == null) continue;
                if (usages ? uses(view, target) : produces(view, target)) out.add(view);
                if (out.size() >= LIMIT) return out;
            }
        }
        return out;
    }

    private static boolean produces(RecipeView v, FabricItemRef target) {
        return v.result() instanceof FabricItemRef r && ItemStack.isSameItem(r.stack(), target.stack());
    }

    private static boolean uses(RecipeView v, FabricItemRef target) {
        for (List<ItemRef> slot : v.slots()) {
            for (ItemRef it : slot) {
                if (it instanceof FabricItemRef f && ItemStack.isSameItem(f.stack(), target.stack())) return true;
            }
        }
        return false;
    }

    private static RecipeView convert(RecipeDisplay display, ContextMap ctx) {
        ItemRef result = first(display.result(), ctx);
        if (result.isEmpty()) return null;
        ItemRef station = first(display.craftingStation(), ctx);
        if (display instanceof ShapedCraftingRecipeDisplay shaped) {
            return new RecipeView("Werkbank", shaped.width(), shaped.height(), resolveAll(shaped.ingredients(), ctx), result, station);
        }
        if (display instanceof ShapelessCraftingRecipeDisplay shapeless) {
            List<List<ItemRef>> slots = resolveAll(shapeless.ingredients(), ctx);
            int n = slots.size();
            int w = n <= 1 ? 1 : n <= 4 ? 2 : 3;
            return new RecipeView("Werkbank (formlos)", w, (n + w - 1) / w, slots, result, station);
        }
        if (display instanceof FurnaceRecipeDisplay furnace) {
            return new RecipeView("Ofen", 1, 1, List.of(resolve(furnace.ingredient(), ctx)), result, station);
        }
        if (display instanceof StonecutterRecipeDisplay cutter) {
            return new RecipeView("Steinsäge", 1, 1, List.of(resolve(cutter.input(), ctx)), result, station);
        }
        return null;
    }

    private static List<List<ItemRef>> resolveAll(List<SlotDisplay> displays, ContextMap ctx) {
        List<List<ItemRef>> out = new ArrayList<>(displays.size());
        for (SlotDisplay d : displays) out.add(resolve(d, ctx));
        return out;
    }

    private static List<ItemRef> resolve(SlotDisplay d, ContextMap ctx) {
        List<ItemRef> out = new ArrayList<>();
        for (ItemStack s : d.resolveForStacks(ctx)) {
            if (!s.isEmpty()) out.add(FabricItemRef.of(s));
            if (out.size() >= 16) break;
        }
        return out;
    }

    private static ItemRef first(SlotDisplay d, ContextMap ctx) {
        List<ItemStack> stacks = d.resolveForStacks(ctx);
        return stacks.isEmpty() ? ItemRef.EMPTY : FabricItemRef.of(stacks.get(0));
    }
}
