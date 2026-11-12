package dev.aio.forge;

import dev.aio.core.platform.ItemRef;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.TextColor;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** {@link ItemRef} over an ItemStack. */
public record ForgeItemRef(ItemStack stack) implements ItemRef {
    private static final Map<String, String> MOD_NAMES = new ConcurrentHashMap<>();

    public static ItemRef of(ItemStack stack) {
        return stack == null || stack.isEmpty() ? ItemRef.EMPTY : new ForgeItemRef(stack);
    }

    @Override public boolean isEmpty() { return stack.isEmpty(); }

    @Override public int count() { return stack.getCount(); }

    @Override public int damage() { return stack.isDamageableItem() ? stack.getDamageValue() : 0; }

    @Override public int maxDamage() { return stack.isDamageableItem() ? stack.getMaxDamage() : 0; }

    @Override public String name() { return stack.getHoverName().getString(); }

    @Override public int nameColor() {
        TextColor c = stack.getStyledHoverName().getStyle().getColor();
        return c == null ? 0xFFFFFFFF : 0xFF000000 | c.getValue();
    }

    @Override public String id() { return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(); }

    @Override public String modName() {
        String id = id();
        String ns = id.contains(":") ? id.substring(0, id.indexOf(':')) : "minecraft";
        return MOD_NAMES.computeIfAbsent(ns, n -> FabricLoader.getInstance().getModContainer(n)
                .map(c -> c.getMetadata().getName())
                .orElse(Character.toUpperCase(n.charAt(0)) + n.substring(1)));
    }

    @Override public int maxStackSize() { return stack.getMaxStackSize(); }

    @Override public boolean sameItemAs(ItemRef other) {
        return other instanceof ForgeItemRef o && !stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, o.stack);
    }

    @Override public List<Enchant> enchantments() {
        ItemEnchantments ench = stack.getEnchantments();
        if (ench.isEmpty()) ench = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (ench.isEmpty()) return List.of();
        List<Enchant> out = new ArrayList<>();
        for (Object2IntMap.Entry<Holder<Enchantment>> e : ench.entrySet()) {
            out.add(new Enchant(e.getKey().value().description().getString(), e.getIntValue(), e.getKey().is(EnchantmentTags.CURSE)));
        }
        return out;
    }
}
