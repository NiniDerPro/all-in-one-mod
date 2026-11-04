package dev.aio.fabric.mixin;

import dev.aio.fabric.Freecam;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Aus Freecam (MIT) übernommen, siehe dev/aio/fabric/freecam/NOTICE.md
@Mixin(Entity.class)
public class FreecamEntityMixin {
    /** Die Kamera schiebt nichts und wird nicht geschoben. */
    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void aio$freecamPush(Entity other, CallbackInfo ci) {
        if (Freecam.active() && (other == Freecam.camera() || (Object) this == Freecam.camera())) ci.cancel();
    }

    /** Blickrichtung des Spielers folgt der Kamera (für Fadenkreuz-Ziel, Blockrahmen usw.). */
    @Inject(method = "getViewXRot", at = @At("HEAD"), cancellable = true)
    private void aio$freecamViewX(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (Freecam.active() && Freecam.camera() != null && (Object) this == Minecraft.getInstance().player) {
            cir.setReturnValue(Freecam.camera().getViewXRot(partialTick));
        }
    }
}
