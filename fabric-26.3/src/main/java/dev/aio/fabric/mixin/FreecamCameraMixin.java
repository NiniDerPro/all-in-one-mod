package dev.aio.fabric.mixin;

import dev.aio.fabric.freecam.FreeCamera;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Aus Freecam (MIT) übernommen, siehe dev/aio/fabric/freecam/NOTICE.md
@Mixin(Camera.class)
public class FreecamCameraMixin {
    @Shadow private Entity entity;
    @Shadow private float eyeHeightOld;
    @Shadow private float eyeHeight;

    /** Beim Umschalten die Augenhöhe sofort übernehmen, ohne Übergang. */
    @Inject(method = "setEntity", at = @At("HEAD"))
    private void aio$freecamSetEntity(Entity entity, CallbackInfo ci) {
        if (entity == null || this.entity == null) return;
        if (entity instanceof FreeCamera || this.entity instanceof FreeCamera) {
            this.eyeHeightOld = this.eyeHeight = entity.getEyeHeight();
        }
    }
}
