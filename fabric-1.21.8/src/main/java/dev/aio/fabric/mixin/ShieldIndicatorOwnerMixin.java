package dev.aio.fabric.mixin;

import dev.aio.fabric.ShieldIndicator;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Shield Indicator: remembers who owns an item when it is captured. */
@Mixin(ItemModelResolver.class)
public abstract class ShieldIndicatorOwnerMixin {
    @Inject(method = "appendItemLayers", at = @At("HEAD"))
    private void aio$shieldOwnerPush(ItemStackRenderState state, ItemStack stack, ItemDisplayContext ctx, Level level,
                                     LivingEntity owner, int seed, CallbackInfo ci) {
        ShieldIndicator.pushOwner(owner);
    }

    @Inject(method = "appendItemLayers", at = @At("RETURN"))
    private void aio$shieldOwnerPop(ItemStackRenderState state, ItemStack stack, ItemDisplayContext ctx, Level level,
                                    LivingEntity owner, int seed, CallbackInfo ci) {
        ShieldIndicator.popOwner();
    }
}
