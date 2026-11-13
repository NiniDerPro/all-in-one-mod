package dev.aio.fabric.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.aio.core.AioCore;
import dev.aio.core.modules.render.NoFogModule;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.level.material.FogType;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** No Fog: pushes the fog out of sight under water, in lava and for the distance fog (each can be switched). */
@Mixin(FogRenderer.class)
public abstract class NoFogMixin {
    @ModifyReturnValue(method = "setupFog", at = @At("RETURN"))
    private static FogParameters aio$noFog(FogParameters fog, Camera camera, FogRenderer.FogMode mode, Vector4f color,
                                           float renderDistance, boolean thick, float partialTick) {
        AioCore core = AioCore.get();
        if (core == null) return fog;
        NoFogModule m = core.module(NoFogModule.class);
        FogType type = camera.getFluidInCamera();
        boolean clear = type == FogType.WATER ? m.clearWater()
                : type == FogType.LAVA ? m.clearLava()
                : type == FogType.NONE && m.noDistanceFog();
        return clear ? new FogParameters(1.0E6F, 1.0E6F, fog.shape(), fog.red(), fog.green(), fog.blue(), fog.alpha()) : fog;
    }
}
