package dev.aio.fabric;

import dev.aio.core.AioCore;
import net.minecraft.network.chat.Component;

/** Öffentlicher Zugang für das Chat-Mixin (die eigentliche Klasse ist paketintern). */
public final class ChatWaypointsBridge {
    private ChatWaypointsBridge() {}

    public static Component rewrite(AioCore core, Component message) {
        try {
            return ChatWaypoints.rewrite(core, message);
        } catch (RuntimeException e) {
            return message; // nie den Chat kaputt machen
        }
    }
}
