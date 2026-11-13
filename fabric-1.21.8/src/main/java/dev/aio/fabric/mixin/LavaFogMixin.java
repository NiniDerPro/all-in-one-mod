package dev.aio.fabric.mixin;

import dev.aio.core.AioCore;
import dev.aio.core.modules.render.NoFogModule;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.LavaFogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** NoFog: klare Lava. */
@Mixin(LavaFogEnvironment.class)
public abstract class LavaFogMixin {
    @Inject(method = "setupFog", at = @At("RETURN"))
    private void aio$clearLava(FogData fog, net.minecraft.world.entity.Entity entity, net.minecraft.core.BlockPos pos, ClientLevel level, float renderDistance, DeltaTracker delta, CallbackInfo ci) {
        AioCore core = AioCore.get();
        if (core != null && core.module(NoFogModule.class).clearLava()) {
            fog.environmentalStart = 1.0E6F;
            fog.environmentalEnd = 1.0E6F;
        }
    }
}
