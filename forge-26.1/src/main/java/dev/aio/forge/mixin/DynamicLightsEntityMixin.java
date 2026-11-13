package dev.aio.forge.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.aio.forge.DynamicLights;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Dynamic Lights: draw mobs and players near a light source brighter. */
@Mixin(EntityRenderer.class)
public abstract class DynamicLightsEntityMixin {
    @ModifyReturnValue(method = "getBlockLightLevel", at = @At("RETURN"))
    private int aio$dynamicEntityLight(int light, Entity entity, BlockPos pos) {
        if (!DynamicLights.lightsEntities()) return light;
        int dyn = DynamicLights.at(entity.getX(), entity.getEyeY(), entity.getZ());
        return Math.max(light, dyn);
    }
}
