package dev.aio.neoforge.mixin;

import dev.aio.core.AioCore;
import dev.aio.core.modules.performance.ParticleLimitModule;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Particles: switched-off types and the percent limit (null = no particle, like vanilla at a full limit). */
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    private void aio$filter(ParticleOptions options, double x, double y, double z, double vx, double vy, double vz,
                            CallbackInfoReturnable<Particle> cir) {
        AioCore core = AioCore.get();
        ParticleLimitModule m = core == null ? null : core.module(ParticleLimitModule.class);
        if (m == null || !m.isActive()) return;
        Identifier id = BuiltInRegistries.PARTICLE_TYPE.getKey(options.getType());
        if (!m.allow(id == null ? null : id.toString())) cir.setReturnValue(null);
    }
}
