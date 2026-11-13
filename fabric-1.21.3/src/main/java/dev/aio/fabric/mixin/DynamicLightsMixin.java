package dev.aio.fabric.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.aio.fabric.DynamicLights;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Dynamic Lights: raise block light by dynamic light when building the chunks. */
@Mixin(LevelRenderer.class)
public abstract class DynamicLightsMixin {
    @ModifyReturnValue(method = "getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I",
            at = @At("RETURN"), require = 0)
    private static int aio$dynamicLight(int coords, BlockAndTintGetter level,
                                        BlockState state, BlockPos pos) {
        int dyn = DynamicLights.atBlock(pos);
        if (dyn <= 0 || state.isSolidRender()) return coords;
        return dyn > LightTexture.block(coords) ? LightTexture.pack(dyn, LightTexture.sky(coords)) : coords;
    }
}
