package dev.aio.fabric;

import dev.aio.core.platform.ItemRef;
import dev.aio.core.platform.RecipeKind;
import dev.aio.core.platform.RecipeView;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;

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
        // In this Minecraft version the server sends its complete recipe list to the client
        return new ArrayList<>(client.level.getRecipeManager().getRecipes());
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
        Object source = client.level;
        List<RecipeView> c = cache;
        if (c != null && source == cacheSource) return c;
        List<RecipeView> list = new ArrayList<>();
        for (RecipeHolder<?> holder : all(client)) {
            RecipeKind kind = kindOf(holder.value().getType());
            RecipeView view = convert(client, holder.value(), kind);
            if (view != null) list.add(view);
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

    private static RecipeView convert(Minecraft client, Recipe<?> recipe, RecipeKind kind) {
        ItemStack out = recipe.getResultItem(client.level.registryAccess());
        if (out == null || out.isEmpty()) return null;
        ItemRef result = FabricItemRef.of(out);
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        if (recipe instanceof ShapedRecipe shaped) {
            return new RecipeView("Crafting", shaped.getWidth(), shaped.getHeight(), resolveAll(ingredients), result,
                    station(Items.CRAFTING_TABLE), RecipeKind.CRAFTING);
        }
        if (recipe instanceof ShapelessRecipe) {
            List<List<ItemRef>> slots = resolveAll(ingredients);
            int n = slots.size();
            int w = n <= 1 ? 1 : n <= 4 ? 2 : 3;
            return new RecipeView("Crafting (shapeless)", w, (n + w - 1) / w, slots, result, station(Items.CRAFTING_TABLE), RecipeKind.CRAFTING);
        }
        if (recipe instanceof AbstractCookingRecipe && !ingredients.isEmpty()) {
            net.minecraft.world.item.Item block = switch (kind) {
                case BLAST_FURNACE -> Items.BLAST_FURNACE;
                case SMOKER -> Items.SMOKER;
                case CAMPFIRE -> Items.CAMPFIRE;
                default -> Items.FURNACE;
            };
            return new RecipeView(furnaceName(kind), 1, 1, List.of(resolve(ingredients.get(0))), result, station(block), kind);
        }
        if (recipe instanceof StonecutterRecipe && !ingredients.isEmpty()) {
            return new RecipeView("Stonecutter", 1, 1, List.of(resolve(ingredients.get(0))), result, station(Items.STONECUTTER), RecipeKind.STONECUTTER);
        }
        return null;
    }

    private static ItemRef station(net.minecraft.world.item.Item item) { return FabricItemRef.of(new ItemStack(item)); }

    private static List<List<ItemRef>> resolveAll(List<Ingredient> ingredients) {
        List<List<ItemRef>> out = new ArrayList<>(ingredients.size());
        for (Ingredient i : ingredients) out.add(resolve(i));
        return out;
    }

    private static List<ItemRef> resolve(Ingredient ingredient) {
        List<ItemRef> out = new ArrayList<>();
        for (ItemStack s : ingredient.getItems()) {
            if (!s.isEmpty()) out.add(FabricItemRef.of(s));
            if (out.size() >= 16) break;
        }
        return out;
    }
}
