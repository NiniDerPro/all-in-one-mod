package dev.aio.fabric;

import dev.aio.core.platform.ItemRef;
import dev.aio.core.platform.RecipeKind;
import dev.aio.core.platform.RecipeView;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimPattern;

import java.util.ArrayList;
import java.util.List;

/**
 * Vorschau einer Rüstungsverzierung: Schmiedevorlage + Rüstungsteil + Material → verzierte Rüstung.
 * Rüstung und Material lassen sich in der Ansicht per Klick durchschalten; das Ergebnis folgt.
 */
final class TrimPreview {
    private static final String SUFFIX = "_armor_trim_smithing_template";

    private TrimPreview() {}

    static List<RecipeView> views(Minecraft client, ItemStack template) {
        Identifier id = BuiltInRegistries.ITEM.getKey(template.getItem());
        if (!id.getPath().endsWith(SUFFIX)) return List.of();
        var access = client.level.registryAccess();
        Identifier patternId = Identifier.fromNamespaceAndPath(id.getNamespace(), id.getPath().substring(0, id.getPath().length() - SUFFIX.length()));
        Holder<TrimPattern> pattern = access.lookupOrThrow(Registries.TRIM_PATTERN)
                .get(net.minecraft.resources.ResourceKey.create(Registries.TRIM_PATTERN, patternId)).orElse(null);
        if (pattern == null) return List.of();

        // Materialien mit dem passenden Item (Vanilla-Namen: eisen → iron_ingot, lapis → lapis_lazuli, …)
        List<Holder<TrimMaterial>> materials = new ArrayList<>();
        List<ItemRef> materialItems = new ArrayList<>();
        access.lookupOrThrow(Registries.TRIM_MATERIAL).listElements().forEach(ref -> {
            Item item = materialItem(ref.key().identifier());
            if (item == null) return;
            materials.add(ref);
            materialItems.add(FabricItemRef.of(new ItemStack(item)));
        });
        List<ItemStack> armor = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            if (stack.is(ItemTags.TRIMMABLE_ARMOR)) armor.add(stack);
        }
        if (materials.isEmpty() || armor.isEmpty()) return List.of();

        List<ItemRef> armorRefs = new ArrayList<>();
        List<ItemRef> results = new ArrayList<>();
        for (ItemStack a : armor) {
            armorRefs.add(FabricItemRef.of(a));
            for (Holder<TrimMaterial> m : materials) {
                ItemStack trimmed = a.copy();
                trimmed.set(DataComponents.TRIM, new ArmorTrim(m, pattern));
                results.add(FabricItemRef.of(trimmed));
            }
        }
        List<List<ItemRef>> slots = List.of(List.of(FabricItemRef.of(template.copyWithCount(1))), armorRefs, materialItems);
        List<String> notes = List.of("Muster: " + pattern.value().description().getString());
        return List.of(new RecipeView("Rüstungsverzierung", 3, 1, slots, ItemRef.EMPTY,
                FabricItemRef.of(new ItemStack(Items.SMITHING_TABLE)), RecipeKind.OTHER, notes, List.of(), results, true));
    }

    private static Item materialItem(Identifier material) {
        String ns = material.getNamespace(), p = material.getPath();
        String[] candidates = {p, p + "_ingot", p + "_shard", p + "_brick", p + "_lazuli", p + "_crystal", p + "_gem"};
        for (String c : candidates) {
            var item = BuiltInRegistries.ITEM.getOptional(Identifier.fromNamespaceAndPath(ns, c));
            if (item.isPresent() && item.get() != Items.AIR) return item.get();
        }
        return null;
    }
}
