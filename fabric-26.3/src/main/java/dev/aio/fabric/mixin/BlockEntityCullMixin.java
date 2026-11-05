package dev.aio.fabric.mixin;

import dev.aio.fabric.Culler;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Verdeckte bzw. weit entfernte Block-Entities (Truhen, Schilder, Banner, …) nicht zeichnen. */
@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityCullMixin {
    @Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true, require = 0)
    private void aio$cull(BlockEntity blockEntity, float partialTick,
                          ModelFeatureRenderer.CrumblingOverlay crumbling,
                          boolean globallyRendered,
                          CallbackInfoReturnable<BlockEntityRenderState> cir) {
        if (blockEntity != null && !globallyRendered && !Culler.shouldRenderBlockEntity(blockEntity)) cir.setReturnValue(null);
    }
}
