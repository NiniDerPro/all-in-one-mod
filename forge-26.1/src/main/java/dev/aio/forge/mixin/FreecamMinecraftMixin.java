package dev.aio.forge.mixin;

import dev.aio.forge.Freecam;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Adapted from Freecam (MIT), see dev/aio/fabric/freecam/NOTICE.md
@Mixin(Minecraft.class)
public class FreecamMinecraftMixin {
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void aio$freecamAttack(CallbackInfoReturnable<Boolean> cir) {
        if (Freecam.active() && Freecam.preventInteractions()) cir.setReturnValue(false);
    }

    @Inject(method = "pickBlockOrEntity", at = @At("HEAD"), cancellable = true)
    private void aio$freecamPick(CallbackInfo ci) {
        if (Freecam.active() && Freecam.preventInteractions()) ci.cancel();
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void aio$freecamBreak(boolean leftClick, CallbackInfo ci) {
        if (Freecam.active() && Freecam.preventInteractions()) ci.cancel();
    }

    /** Switch off when leaving the world. */
    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V", at = @At("HEAD"))
    private void aio$freecamDisconnect(CallbackInfo ci) {
        Freecam.set(false);
    }
}
