package dev.aio.fabric.mixin;

import dev.aio.fabric.freecam.FreeCamera;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Adapted from Freecam (MIT), see dev/aio/fabric/freecam/NOTICE.md
@Mixin(Camera.class)
public class FreecamCameraMixin {
    @Shadow private Entity entity;
    @Shadow private float eyeHeightOld;
    @Shadow private float eyeHeight;

    /** When switching take over the eye height immediately, without transition. */
    @Inject(method = "setup", at = @At("HEAD"))
    private void aio$freecamSetEntity(CallbackInfo ci, @Local(argsOnly = true) Entity entity) {
        if (entity == null || this.entity == null) return;
        if (entity instanceof FreeCamera || this.entity instanceof FreeCamera) {
            this.eyeHeightOld = this.eyeHeight = entity.getEyeHeight();
        }
    }
}
