package dev.aio.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/** NeoForge entry point. The mod is client-side only: on a dedicated server it is not loaded. */
@Mod(value = "aio", dist = Dist.CLIENT)
public final class AioNeoForgeMod {
    public AioNeoForgeMod(IEventBus modBus) {
        AioNeoForgeClient.init(modBus);
    }
}
