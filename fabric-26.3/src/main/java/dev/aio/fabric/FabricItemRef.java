package dev.aio.fabric;

import dev.aio.core.platform.ItemRef;
import net.minecraft.world.item.ItemStack;

/** {@link ItemRef} über einem ItemStack. */
public record FabricItemRef(ItemStack stack) implements ItemRef {
    public static ItemRef of(ItemStack stack) {
        return stack == null || stack.isEmpty() ? ItemRef.EMPTY : new FabricItemRef(stack);
    }

    @Override public boolean isEmpty() { return stack.isEmpty(); }

    @Override public int count() { return stack.getCount(); }

    @Override public int damage() { return stack.isDamageableItem() ? stack.getDamageValue() : 0; }

    @Override public int maxDamage() { return stack.isDamageableItem() ? stack.getMaxDamage() : 0; }

    @Override public String name() { return stack.getHoverName().getString(); }
}
