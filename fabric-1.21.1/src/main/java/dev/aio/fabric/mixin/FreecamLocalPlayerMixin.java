package dev.aio.fabric.mixin;

import dev.aio.fabric.Freecam;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Adapted from Freecam (MIT), see dev/aio/fabric/freecam/NOTICE.md
@Mixin(LocalPlayer.class)
public class FreecamLocalPlayerMixin {
    @Inject(method = "isControlledCamera", at = @At("HEAD"), cancellable = true, require = 0)
    private void aio$freecamControlled(CallbackInfoReturnable<Boolean> cir) {
        if (Freecam.active()) cir.setReturnValue(true);
    }

    @Inject(method = "getViewYRot", at = @At("HEAD"), cancellable = true)
    private void aio$freecamViewY(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (Freecam.active() && Freecam.camera() != null) cir.setReturnValue(Freecam.camera().getViewYRot(partialTick));
    }
}
