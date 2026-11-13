package dev.aio.fabric.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.aio.core.AioCore;
import dev.aio.core.modules.render.NoFogModule;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No Fog: pushes the fog out of sight under water, in lava and for the distance fog (each can be switched). */
@Mixin(FogRenderer.class)
public abstract class NoFogMixin {
    @Inject(method = "setupFog", at = @At("TAIL"))
    private static void aio$noFog(Camera camera, FogRenderer.FogMode mode, float renderDistance, boolean thick, float partialTick, CallbackInfo ci) {
        AioCore core = AioCore.get();
        if (core == null) return;
        NoFogModule m = core.module(NoFogModule.class);
        FogType type = camera.getFluidInCamera();
        boolean clear = type == FogType.WATER ? m.clearWater()
                : type == FogType.LAVA ? m.clearLava()
                : type == FogType.NONE && m.noDistanceFog();
        if (clear) {
            RenderSystem.setShaderFogStart(1.0E6F);
            RenderSystem.setShaderFogEnd(1.0E6F);
        }
    }
}
