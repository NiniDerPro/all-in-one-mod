package dev.aio.neoforge.mixin;

import dev.aio.core.AioCore;
import dev.aio.core.modules.render.NoFogModule;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** NoFog: no fog at the edge of the view distance, in overworld, nether and end (all go through here). */
@Mixin(AtmosphericFogEnvironment.class)
public abstract class AtmosphericFogMixin {
    @Inject(method = "setupFog", at = @At("TAIL"))
    private void aio$noDistanceFog(FogData fog, Camera camera, ClientLevel level, float renderDistance, DeltaTracker delta, CallbackInfo ci) {
        AioCore core = AioCore.get();
        if (core != null && core.module(NoFogModule.class).noDistanceFog()) {
            fog.environmentalStart = 1.0E6F;
            fog.environmentalEnd = 1.0E6F;
            fog.renderDistanceStart = 1.0E6F;
            fog.renderDistanceEnd = 1.0E6F;
            fog.skyEnd = 1.0E6F;
            fog.cloudEnd = 1.0E6F;
        }
    }
}
