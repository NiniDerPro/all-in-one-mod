package dev.aio.fabric.mixin;

import dev.aio.fabric.ShieldIndicator;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Shield Indicator: remembers who holds an item while it is drawn. */
@Mixin(ItemRenderer.class)
public abstract class ShieldIndicatorOwnerMixin {
    private static final String RENDER = "renderStatic(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;III)V";

    @Inject(method = RENDER, at = @At("HEAD"))
    private void aio$shieldOwnerPush(CallbackInfo ci, @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) LivingEntity owner) {
        ShieldIndicator.pushOwner(owner);
    }

    @Inject(method = RENDER, at = @At("RETURN"))
    private void aio$shieldOwnerPop(CallbackInfo ci) {
        ShieldIndicator.popOwner();
    }
}
