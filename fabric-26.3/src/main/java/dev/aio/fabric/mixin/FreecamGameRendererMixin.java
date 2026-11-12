package dev.aio.fabric.mixin;

import dev.aio.fabric.Freecam;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Adapted from Freecam (MIT), see dev/aio/fabric/freecam/NOTICE.md
@Mixin(GameRenderer.class)
public class FreecamGameRendererMixin {
    /** Hand in der Freecam ausblenden (Einstellung "Show hand"). */
    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void aio$freecamHand(CallbackInfo ci) {
        if (Freecam.active() && Freecam.hideHand()) ci.cancel();
    }

    /** No block outline while interaction is not allowed. */
    @Inject(method = "shouldRenderBlockOutline", at = @At("HEAD"), cancellable = true)
    private void aio$freecamOutline(CallbackInfoReturnable<Boolean> cir) {
        if (Freecam.active() && Freecam.preventInteractions()) cir.setReturnValue(false);
    }
}
