package dev.aio.core.platform;

import java.util.List;

/**
 * Ein Objekt mit Namensschild: Punkt über dem Kopf und was angezeigt wird.
 *
 * @param items     bei Spielern: Haupthand, Helm, Brust, Beine, Füße, Zweithand (leere als {@link ItemRef#EMPTY})
 * @param count     bei Items am Boden: Stapelgröße
 * @param fuseTicks bei TNT: verbleibende Ticks
 */
public record NametagTarget(Kind kind, double x, double y, double z, double distance, String name, int nameColor,
                            float health, float maxHealth, float absorption, int ping, String gameMode,
                            List<ItemRef> items, int count, int fuseTicks) {
    public enum Kind { PLAYER, MOB, ITEM, TNT }
}
