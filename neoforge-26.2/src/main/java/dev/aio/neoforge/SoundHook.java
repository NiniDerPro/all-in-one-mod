package dev.aio.neoforge;

import dev.aio.core.AioCore;
import dev.aio.core.event.Events;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Passes sounds that start playing in the world to the core (Sound Radar). */
public final class SoundHook {
    private static boolean installed;

    private SoundHook() {}

    public static void install(Minecraft mc, AioCore core) {
        if (installed || mc == null || mc.getSoundManager() == null) return;
        installed = true;
        mc.getSoundManager().addListener((sound, events, range) -> {
            if (sound.isRelative()) return;
            Component sub = events.getSubtitle();
            core.bus().post(new Events.Sound(sound.getIdentifier().toString(), sub == null ? "" : sub.getString(),
                    sound.getSource().getName(), sound.getX(), sound.getY(), sound.getZ()));
        });
    }
}
