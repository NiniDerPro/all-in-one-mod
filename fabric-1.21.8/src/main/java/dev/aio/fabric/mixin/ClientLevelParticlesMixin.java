package dev.aio.fabric.mixin;

import dev.aio.core.AioCore;
import dev.aio.core.modules.performance.ParticleLimitModule;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Particles without their own particle type: block splinters on breaking and mining, fireworks. */
@Mixin(ClientLevel.class)
public abstract class ClientLevelParticlesMixin {
    @Unique
    private static boolean aio$off(String id) {
        AioCore core = AioCore.get();
        ParticleLimitModule m = core == null ? null : core.module(ParticleLimitModule.class);
        return m != null && m.isActive() && m.isDisabled(id);
    }

    @Inject(method = "addDestroyBlockEffect", at = @At("HEAD"), cancellable = true)
    private void aio$blockBreak(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (aio$off(ParticleLimitModule.BLOCK_BREAK)) ci.cancel();
    }


    @Inject(method = "createFireworks", at = @At("HEAD"), cancellable = true)
    private void aio$fireworks(double x, double y, double z, double vx, double vy, double vz,
                               List<FireworkExplosion> explosions,
                               CallbackInfo ci) {
        if (aio$off("minecraft:firework")) ci.cancel();
    }
}
