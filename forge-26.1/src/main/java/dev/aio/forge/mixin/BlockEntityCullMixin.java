package dev.aio.forge.mixin;

import dev.aio.forge.Culler;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Do not draw hidden or distant block entities (chests, signs, banners, …). */
@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityCullMixin {
    @Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true, require = 0)
    private void aio$cull(BlockEntity blockEntity, float partialTick,
                          ModelFeatureRenderer.CrumblingOverlay crumbling,
                          CallbackInfoReturnable<BlockEntityRenderState> cir) {
        if (blockEntity != null && !Culler.shouldRenderBlockEntity(blockEntity)) cir.setReturnValue(null);
    }
}
