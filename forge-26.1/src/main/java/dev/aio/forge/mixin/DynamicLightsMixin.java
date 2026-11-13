package dev.aio.forge.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.aio.forge.DynamicLights;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Dynamic Lights: raise block light by dynamic light when building the chunks. */
@Mixin(LevelRenderer.class)
public abstract class DynamicLightsMixin {
    @ModifyReturnValue(method = "getLightCoords(Lnet/minecraft/client/renderer/LevelRenderer$BrightnessGetter;Lnet/minecraft/world/level/BlockAndLightGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I",
            at = @At("RETURN"))
    private static int aio$dynamicLight(int coords, LevelRenderer.BrightnessGetter getter, BlockAndLightGetter level,
                                        BlockState state, BlockPos pos) {
        int dyn = DynamicLights.atBlock(pos);
        if (dyn <= 0 || state.isSolidRender()) return coords;
        return dyn > LightCoordsUtil.block(coords) ? LightCoordsUtil.withBlock(coords, dyn) : coords;
    }
}
