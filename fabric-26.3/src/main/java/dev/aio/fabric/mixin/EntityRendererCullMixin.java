package dev.aio.fabric.mixin;

import dev.aio.fabric.Culler;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Entity culling and render distance: do not draw hidden or too distant entities (checked after the view test). */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererCullMixin {
    @Inject(method = "shouldRender", at = @At("RETURN"), cancellable = true, require = 0)
    private void aio$cull(Entity entity, Frustum frustum, double camX, double camY, double camZ, float partialTick,
                          CallbackInfoReturnable<Boolean> cir) {
        // only for what Minecraft would draw (in the view): no rays for mobs behind the camera
        if (cir.getReturnValueZ() && !Culler.shouldRenderEntity(entity)) cir.setReturnValue(false);
    }
}
