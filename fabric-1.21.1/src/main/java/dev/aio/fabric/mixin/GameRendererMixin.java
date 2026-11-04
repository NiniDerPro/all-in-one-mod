package dev.aio.fabric.mixin;

import dev.aio.core.AioCore;
import dev.aio.core.modules.render.ZoomModule;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Zoom: verkleinert das Sichtfeld der Welt (nicht das der Hand). */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void aio$zoom(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        if (!changingFov) return;
        AioCore core = AioCore.get();
        if (core == null) return;
        double multiplier = core.module(ZoomModule.class).fovMultiplier();
        if (multiplier != 1) cir.setReturnValue(cir.getReturnValue() * multiplier);
    }
}
