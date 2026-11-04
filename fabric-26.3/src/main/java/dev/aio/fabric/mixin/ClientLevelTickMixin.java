package dev.aio.fabric.mixin;

import dev.aio.fabric.Culler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Verdeckte Entities auf dem Client nur vereinfacht ticken (Position nachführen, keine Animationen). */
@Mixin(ClientLevel.class)
public abstract class ClientLevelTickMixin {
    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true, require = 0)
    private void aio$tickCull(Entity entity, CallbackInfo ci) {
        if (!Culler.skipTick(entity)) return;
        entity.setOldPosAndRot();
        entity.getInterpolation().interpolate();
        entity.tickCount++;
        if (entity instanceof LivingEntity living && living.hurtTime > 0) living.hurtTime--;
        ci.cancel();
    }
}
