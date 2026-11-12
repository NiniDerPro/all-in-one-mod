package dev.aio.fabric.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.aio.core.AioCore;
import dev.aio.core.modules.render.FullbrightModule;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Fullbright: raises the brightness of the lightmap without touching the gamma option (nothing ends up in options.txt). */
@Mixin(LightTexture.class)
public abstract class LightmapMixin {
    /** The third OptionInstance.get() in updateLightTexture is the gamma option. */
    @ModifyExpressionValue(method = "updateLightTexture", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;", ordinal = 2))
    private Object aio$fullbright(Object original) {
        AioCore core = AioCore.get();
        if (core == null) return original;
        double gamma = core.module(FullbrightModule.class).gammaOverride();
        return Double.isNaN(gamma) ? original : (Object) Double.valueOf(gamma);
    }
}
