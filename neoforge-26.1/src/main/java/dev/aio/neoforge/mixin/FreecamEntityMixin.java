package dev.aio.neoforge.mixin;

import dev.aio.neoforge.Freecam;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Adapted from Freecam (MIT), see dev/aio/fabric/freecam/NOTICE.md
@Mixin(Entity.class)
public class FreecamEntityMixin {
    /** The camera pushes nothing and is not pushed. */
    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void aio$freecamPush(Entity other, CallbackInfo ci) {
        if (Freecam.active() && (other == Freecam.camera() || (Object) this == Freecam.camera())) ci.cancel();
    }

    /** The player's viewing direction follows the camera (for crosshair target, block outline etc.). */
    @Inject(method = "getViewXRot", at = @At("HEAD"), cancellable = true)
    private void aio$freecamViewX(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (Freecam.active() && Freecam.camera() != null && (Object) this == Minecraft.getInstance().player) {
            cir.setReturnValue(Freecam.camera().getViewXRot(partialTick));
        }
    }
}
