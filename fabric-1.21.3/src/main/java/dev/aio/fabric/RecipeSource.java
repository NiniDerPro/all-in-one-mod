package dev.aio.fabric;

import dev.aio.core.platform.ItemRef;
import dev.aio.core.platform.RecipeKind;
import dev.aio.core.platform.RecipeView;
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
 * Recipes for the item list. Source: in singleplayer the integrated server, otherwise the
 * default recipes of Minecraft (the Fabric API of this version has no recipe sync to the client).
 */
final class RecipeSource {
    private static final int LIMIT = 1000;
    /** Converted recipes of the current source; once per world/source instead of on every open. */
    private static volatile List<RecipeView> cache;
    private static volatile Object cacheSource;

    private RecipeSource() {}

    static void clear() {
        cache = null;
        cacheSource = null;
        VanillaRecipes.clear();
    }

    /** Always available: if necessary the vanilla recipes from the game itself. */
    static boolean available(Minecraft client) { return client.level != null; }

    private static Collection<RecipeHolder<?>> all(Minecraft client) {
        var server = client.getSingleplayerServer();
        if (server != null) {
            List<RecipeHolder<?>> out = new ArrayList<>(server.getRecipeManager().getRecipes());
            return out;
        }
        return VanillaRecipes.get(client);   // otherwise the default recipes of Minecraft
    }

    static List<RecipeView> find(Minecraft client, ItemRef item, boolean usages) {
        if (client.level == null || !(item instanceof FabricItemRef target)) return List.of();
        List<RecipeView> out = new ArrayList<>();
        for (RecipeView view : views(client)) {
            if (usages ? uses(view, target) : produces(view, target)) out.add(view);
            if (out.size() >= LIMIT) break;
        }
        return out;
    }

    /** All recipes of the current source as display data (cached, source = server/sync/vanilla). */
    private static List<RecipeView> views(Minecraft client) {
        Object source = client.getSingleplayerServer() != null ? client.getSingleplayerServer() : client.level;
        List<RecipeView> c = cache;
        if (c != null && source == cacheSource) return c;
        ContextMap ctx = SlotDisplayContext.fromLevel(client.level);
        List<RecipeView> list = new ArrayList<>();
        for (RecipeHolder<?> holder : all(client)) {
            RecipeKind kind = kindOf(holder.value().getType());
            for (RecipeDisplay display : holder.value().display()) {
                RecipeView view = convert(display, ctx, kind);
                if (view != null) list.add(view);
            }
        }
        cache = List.copyOf(list);
        cacheSource = source;
        return cache;
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

    private static RecipeKind kindOf(RecipeType<?> type) {
        if (type == RecipeType.CRAFTING) return RecipeKind.CRAFTING;
        if (type == RecipeType.SMELTING) return RecipeKind.FURNACE;
        if (type == RecipeType.BLASTING) return RecipeKind.BLAST_FURNACE;
        if (type == RecipeType.SMOKING) return RecipeKind.SMOKER;
        if (type == RecipeType.CAMPFIRE_COOKING) return RecipeKind.CAMPFIRE;
        if (type == RecipeType.STONECUTTING) return RecipeKind.STONECUTTER;
        return RecipeKind.OTHER;
    }

    private static String furnaceName(RecipeKind kind) {
        return switch (kind) {
            case BLAST_FURNACE -> "Blast Furnace";
            case SMOKER -> "Smoker";
            case CAMPFIRE -> "Campfire";
            default -> "Furnace";
        };
    }

    private static RecipeView convert(RecipeDisplay display, ContextMap ctx, RecipeKind kind) {
        ItemRef result = first(display.result(), ctx);
        if (result.isEmpty()) return null;
        ItemRef station = first(display.craftingStation(), ctx);
        if (display instanceof ShapedCraftingRecipeDisplay shaped) {
            return new RecipeView("Crafting", shaped.width(), shaped.height(), resolveAll(shaped.ingredients(), ctx), result, station, RecipeKind.CRAFTING);
        }
        if (display instanceof ShapelessCraftingRecipeDisplay shapeless) {
            List<List<ItemRef>> slots = resolveAll(shapeless.ingredients(), ctx);
            int n = slots.size();
            int w = n <= 1 ? 1 : n <= 4 ? 2 : 3;
            return new RecipeView("Crafting (shapeless)", w, (n + w - 1) / w, slots, result, station, RecipeKind.CRAFTING);
        }
        if (display instanceof FurnaceRecipeDisplay furnace) {
            return new RecipeView(furnaceName(kind), 1, 1, List.of(resolve(furnace.ingredient(), ctx)), result, station, kind);
        }
        if (display instanceof StonecutterRecipeDisplay cutter) {
            return new RecipeView("Stonecutter", 1, 1, List.of(resolve(cutter.input(), ctx)), result, station, RecipeKind.STONECUTTER);
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
