package dev.aio.forge.mixin;

import dev.aio.forge.Culler;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Entity culling and render distance: do not draw hidden or too distant entities. */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererCullMixin {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true, require = 0)
    private void aio$cull(Entity entity, Frustum frustum, double camX, double camY, double camZ, float partialTick,
                          CallbackInfoReturnable<Boolean> cir) {
        if (!Culler.shouldRenderEntity(entity)) cir.setReturnValue(false);
    }
}
