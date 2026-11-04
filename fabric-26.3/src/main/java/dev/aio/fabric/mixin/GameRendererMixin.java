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
 * No Hurtcam: überspringt das Kamerawackeln bei Schaden. Signatur nach dem Vorbild von
 * {@code bobView(CameraRenderState, PoseStack)} (belegt); require = 0, damit ein abweichender
 * Name nur die Funktion kostet, nicht den Start.
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
