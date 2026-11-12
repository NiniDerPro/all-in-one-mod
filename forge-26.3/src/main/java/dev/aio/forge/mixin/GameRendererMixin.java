package dev.aio.forge.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.aio.core.AioCore;
import dev.aio.core.modules.render.NoHurtcamModule;
import dev.aio.core.modules.render.NoViewBobbingModule;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * No Hurtcam (the shake can be reduced instead of removed) and No View Bobbing. Signature of
 * {@code bobHurt/bobView(CameraRenderState, PoseStack)} (confirmed); require = 0 so that a deviating
 * name only costs the feature, not the start.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @WrapOperation(method = {"renderLevel", "renderItemInHand"}, require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;bobHurt(Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V"))
    private void aio$noHurtcam(GameRenderer instance, CameraRenderState camera, PoseStack pose, Operation<Void> original) {
        AioCore core = AioCore.get();
        double keep = core == null ? 1 : core.module(NoHurtcamModule.class).remaining();
        if (keep >= 1) {
            original.call(instance, camera, pose);
        } else if (keep > 0) {
            // Let the game compute the shake on a scratch stack, then apply only a part of that rotation
            PoseStack scratch = new PoseStack();
            original.call(instance, camera, scratch);
            Quaternionf shake = scratch.last().pose().getNormalizedRotation(new Quaternionf());
            pose.rotate(new Quaternionf().slerp(shake, (float) keep));
        }
    }

    @WrapOperation(method = {"renderLevel", "renderItemInHand"}, require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;bobView(Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V"))
    private void aio$noViewBobbing(GameRenderer instance, CameraRenderState camera, PoseStack pose, Operation<Void> original) {
        AioCore core = AioCore.get();
        if (core != null && core.module(NoViewBobbingModule.class).suppress()) return;
        original.call(instance, camera, pose);
    }
}
