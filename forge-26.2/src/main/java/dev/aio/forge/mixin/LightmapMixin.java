package dev.aio.forge.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.aio.core.AioCore;
import dev.aio.core.modules.render.FullbrightModule;
import net.minecraft.client.renderer.Lightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static org.objectweb.asm.Opcodes.GETFIELD;

/** Fullbright: raises the brightness of the lightmap without touching the gamma option (nothing ends up in options.txt). */
@Mixin(Lightmap.class)
public abstract class LightmapMixin {
    @ModifyExpressionValue(method = "render",
            at = @At(value = "FIELD", opcode = GETFIELD, target = "Lnet/minecraft/client/renderer/state/LightmapRenderState;brightness:F"))
    private float aio$fullbright(float original) {
        AioCore core = AioCore.get();
        if (core == null) return original;
        double gamma = core.module(FullbrightModule.class).gammaOverride();
        return Double.isNaN(gamma) ? original : (float) gamma;
    }
}
