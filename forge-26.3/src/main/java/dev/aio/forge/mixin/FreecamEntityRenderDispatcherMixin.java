package dev.aio.forge.mixin;

import dev.aio.forge.freecam.FreeCamera;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Adapted from Freecam (MIT), see dev/aio/fabric/freecam/NOTICE.md
@Mixin(EntityRenderDispatcher.class)
public class FreecamEntityRenderDispatcherMixin {
    /** Never draw the camera body itself. */
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void aio$freecamHideCamera(Entity entity, Frustum frustum, double x, double y, double z, float partialTicks,
                                       CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof FreeCamera) cir.setReturnValue(false);
    }
}
