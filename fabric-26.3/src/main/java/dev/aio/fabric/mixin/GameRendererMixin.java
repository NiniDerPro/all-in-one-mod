package dev.aio.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.aio.core.AioCore;
import dev.aio.core.modules.render.NoHurtcamModule;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * No Hurtcam: skips the camera shake on damage. Signature of
 * {@code bobView(CameraRenderState, PoseStack)} (confirmed); require = 0 so that a deviating
 * name only costs the feature, not the start.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @WrapOperation(method = "renderLevel", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;bobHurt(Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V"))
    private void aio$noHurtcam(GameRenderer instance, CameraRenderState camera, PoseStack pose, Operation<Void> original) {
        AioCore core = AioCore.get();
        if (core != null && core.module(NoHurtcamModule.class).suppress()) return;
        original.call(instance, camera, pose);
    }
}
