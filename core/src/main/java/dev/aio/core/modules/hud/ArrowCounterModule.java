package dev.aio.core.modules.hud;

public final class ArrowCounterModule extends ItemCounterModule {
    public ArrowCounterModule() {
        super("arrows", "Pfeil-Zähler", "Alle Pfeile im Inventar (auch Spektral- und Trankpfeile)", "#minecraft:arrows",
                "minecraft:arrow", 1.0, 0.68);
    }
}
