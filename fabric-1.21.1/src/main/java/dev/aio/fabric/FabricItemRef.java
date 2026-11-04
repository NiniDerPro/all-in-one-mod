package dev.aio.fabric;

import dev.aio.core.platform.ItemRef;
import net.minecraft.item.ItemStack;

/** {@link ItemRef} über einem ItemStack. */
public record FabricItemRef(ItemStack stack) implements ItemRef {
    public static ItemRef of(ItemStack stack) {
        return stack == null || stack.isEmpty() ? ItemRef.EMPTY : new FabricItemRef(stack);
    }

    @Override public boolean isEmpty() { return stack.isEmpty(); }

    @Override public int count() { return stack.getCount(); }

    @Override public int damage() { return stack.isDamageable() ? stack.getDamage() : 0; }

    @Override public int maxDamage() { return stack.isDamageable() ? stack.getMaxDamage() : 0; }

    @Override public String name() { return stack.getName().getString(); }
}
