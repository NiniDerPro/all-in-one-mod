package dev.aio.core.platform;

import java.util.List;

/** Version-independent view of an ItemStack. */
public interface ItemRef {
    ItemRef EMPTY = new ItemRef() {
        @Override public boolean isEmpty() { return true; }
        @Override public int count() { return 0; }
        @Override public int damage() { return 0; }
        @Override public int maxDamage() { return 0; }
        @Override public String name() { return ""; }
    };

    boolean isEmpty();

    int count();

    int damage();

    int maxDamage();

    String name();

    /** Registry-ID, z. B. {@code minecraft:diamond_sword}. */
    default String id() { return ""; }

    /** Display name of the mod the item comes from. */
    default String modName() { return ""; }

    /** Colour of the name as in the tooltip (rarity, enchanted = cyan, …), ARGB. */
    default int nameColor() { return 0xFFFFFFFF; }

    default int maxStackSize() { return 64; }

    /** Gleiches Item mit gleichen Komponenten (stapelbar). */
    default boolean sameItemAs(ItemRef other) {
        return other != null && !isEmpty() && !other.isEmpty() && id().equals(other.id()) && name().equals(other.name());
    }

    default boolean isDamageable() { return maxDamage() > 0; }

    /** Verzauberung mit Stufe; {@code curse} = Fluch (rot anzeigen). */
    record Enchant(String name, int level, boolean curse) {}

    default List<Enchant> enchantments() { return List.of(); }

    default int durability() { return maxDamage() - damage(); }

    default float durabilityFraction() {
        return maxDamage() <= 0 ? 1f : Math.max(0f, Math.min(1f, durability() / (float) maxDamage()));
    }
}
