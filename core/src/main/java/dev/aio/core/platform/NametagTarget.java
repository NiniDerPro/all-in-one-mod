package dev.aio.core.platform;

import java.util.List;

/**
 * An object with a nametag: point above the head and what is shown.
 *
 * @param items     for players: main hand, helmet, chest, legs, feet, offhand (empty ones as {@link ItemRef#EMPTY})
 * @param count     for items on the ground: stack size
 * @param fuseTicks for TNT: remaining ticks
 */
public record NametagTarget(Kind kind, double x, double y, double z, double distance, String name, int nameColor,
                            float health, float maxHealth, float absorption, int ping, String gameMode,
                            List<ItemRef> items, int count, int fuseTicks) {
    public enum Kind { PLAYER, MOB, ITEM, TNT }
}
