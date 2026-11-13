package dev.aio.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

/** Forge entry point. The mod is client-side only: on a dedicated server it does nothing. */
@Mod("aio")
public final class AioForgeMod {
    public AioForgeMod() {
        if (FMLEnvironment.dist == Dist.CLIENT) AioForgeClient.init();
    }
}
