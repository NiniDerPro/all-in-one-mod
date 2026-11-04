package dev.aio.core.modules.hud;

public final class ArrowCounterModule extends ItemCounterModule {
    public ArrowCounterModule() {
        super("arrows", "Arrow Counter", "All arrows in your inventory (including spectral and tipped)", "#minecraft:arrows",
                "minecraft:arrow", 1.0, 0.68);
    }
}
