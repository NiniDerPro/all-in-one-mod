package dev.aio.forge.mixin;

import dev.aio.forge.Culler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hidden entities only tick in a simplified way on the client (follow position, no animations). */
@Mixin(ClientLevel.class)
public abstract class ClientLevelTickMixin {
    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true, require = 0)
    private void aio$tickCull(Entity entity, CallbackInfo ci) {
        if (!Culler.skipTick(entity)) return;
        entity.setOldPosAndRot();
        var interpolation = entity.getInterpolation(); // null for entity types without position interpolation
        if (interpolation != null) interpolation.interpolate();
        entity.tickCount++;
        if (entity instanceof LivingEntity living && living.hurtTime > 0) living.hurtTime--;
        ci.cancel();
    }
}
