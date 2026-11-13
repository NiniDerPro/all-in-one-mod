package dev.aio.forge.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.aio.forge.shim.LevelRenderEvents;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fires {@link LevelRenderEvents}: own geometry after the entities, and the block outline replacement. */
@Mixin(LevelRenderer.class)
public abstract class LevelRenderHooksMixin {
    @Inject(method = "submitTransientBlocks", at = @At("TAIL"), require = 0)
    private void aio$collectSubmits(PoseStack poseStack, SubmitNodeCollector collector, LevelRenderState state, CallbackInfo ci) {
        var context = new LevelRenderEvents.Context(state, poseStack, collector);
        for (var l : LevelRenderEvents.COLLECT_SUBMITS.list) l.collect(context);
    }

    @Inject(method = "submitBlockOutline", at = @At("HEAD"), cancellable = true, require = 0)
    private void aio$blockOutline(PoseStack poseStack, SubmitNodeCollector collector, LevelRenderState state, CallbackInfo ci) {
        BlockOutlineRenderState outline = state.blockOutlineRenderState;
        if (outline == null) return;
        var context = new LevelRenderEvents.Context(state, poseStack, collector);
        for (var l : LevelRenderEvents.BEFORE_BLOCK_OUTLINE.list) {
            if (!l.beforeOutline(context, outline)) {
                ci.cancel();
                return;
            }
        }
    }
}
