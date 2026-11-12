package dev.aio.fabric.mixin;

import dev.aio.core.AioCore;
import dev.aio.core.modules.render.NoFogModule;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.WaterFogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** NoFog: klares Wasser. */
@Mixin(WaterFogEnvironment.class)
public abstract class WaterFogMixin {
    @Inject(method = "setupFog", at = @At("RETURN"))
    private void aio$clearWater(FogData fog, Camera camera, ClientLevel level, float renderDistance, DeltaTracker delta, CallbackInfo ci) {
        AioCore core = AioCore.get();
        if (core != null && core.module(NoFogModule.class).clearWater()) {
            fog.environmentalStart = 1.0E6F;
            fog.environmentalEnd = 1.0E6F;
        }
    }
}
