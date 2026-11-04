package dev.aio.core.platform;

/** Versionsunabhängige Sicht auf einen ItemStack. */
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

    default boolean isDamageable() { return maxDamage() > 0; }

    default int durability() { return maxDamage() - damage(); }

    default float durabilityFraction() {
        return maxDamage() <= 0 ? 1f : Math.max(0f, Math.min(1f, durability() / (float) maxDamage()));
    }
}
