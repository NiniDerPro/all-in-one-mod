package dev.aio.fabric.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.aio.core.AioCore;
import dev.aio.core.modules.render.NoHurtcamModule;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No Hurtcam: skips the camera shake on damage. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true, require = 0)
    private void aio$noHurtcam(PoseStack pose, float partialTicks, CallbackInfo ci) {
        AioCore core = AioCore.get();
        if (core != null && core.module(NoHurtcamModule.class).suppress()) ci.cancel();
    }
}
