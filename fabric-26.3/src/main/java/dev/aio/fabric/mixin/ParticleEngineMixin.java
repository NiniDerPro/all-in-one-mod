package dev.aio.fabric.mixin;

import dev.aio.core.AioCore;
import dev.aio.core.modules.performance.ParticleLimitModule;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Partikel-Limit: einen Teil der neuen Partikel verwerfen. */
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
    @Inject(method = "add", at = @At("HEAD"), cancellable = true, require = 0)
    private void aio$limit(Particle particle, CallbackInfo ci) {
        AioCore core = AioCore.get();
        ParticleLimitModule m = core == null ? null : core.module(ParticleLimitModule.class);
        if (m != null && !m.allow()) ci.cancel();
    }
}
