package dev.aio.fabric.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.aio.fabric.Culler;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Do not draw hidden or distant block entities (chests, signs, banners, ...). */
@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityCullMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void aio$cull(BlockEntity blockEntity, float partialTick, PoseStack pose, MultiBufferSource buffers, CallbackInfo ci) {
        if (blockEntity != null && !Culler.shouldRenderBlockEntity(blockEntity)) ci.cancel();
    }
}
