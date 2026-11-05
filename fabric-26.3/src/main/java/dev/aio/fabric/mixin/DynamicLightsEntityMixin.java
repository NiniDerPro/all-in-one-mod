package dev.aio.fabric.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.aio.fabric.DynamicLights;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Dynamic Lights: Mobs und Spieler in der Nähe einer Lichtquelle heller zeichnen. */
@Mixin(EntityRenderer.class)
public abstract class DynamicLightsEntityMixin {
    @ModifyReturnValue(method = "getBlockLightLevel", at = @At("RETURN"))
    private int aio$dynamicEntityLight(int light, Entity entity, BlockPos pos) {
        if (!DynamicLights.lightsEntities()) return light;
        int dyn = DynamicLights.at(entity.getX(), entity.getEyeY(), entity.getZ());
        return Math.max(light, dyn);
    }
}
