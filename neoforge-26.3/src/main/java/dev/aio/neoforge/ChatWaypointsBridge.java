package dev.aio.neoforge;

import dev.aio.core.AioCore;
import net.minecraft.network.chat.Component;

/** Public access for the chat mixin (the actual class is package-private). */
public final class ChatWaypointsBridge {
    private ChatWaypointsBridge() {}

    public static Component rewrite(AioCore core, Component message) {
        try {
            return ChatWaypoints.rewrite(core, message);
        } catch (RuntimeException e) {
            return message; // never break the chat
        }
    }
}
