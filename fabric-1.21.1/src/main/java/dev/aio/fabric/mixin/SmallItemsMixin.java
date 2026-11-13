package dev.aio.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.aio.core.AioCore;
import dev.aio.core.modules.render.SmallItemsModule;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Small Shield & Totem: draw the item in hand smaller and a bit lower. */
@Mixin(ItemInHandRenderer.class)
public abstract class SmallItemsMixin {
    @WrapOperation(method = "renderArmWithItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
    private void aio$smallItems(ItemInHandRenderer self, LivingEntity entity, ItemStack stack, ItemDisplayContext context, PoseStack pose,
                                MultiBufferSource buffers, int light, Operation<Void> original) {
        AioCore core = AioCore.get();
        SmallItemsModule m = core == null ? null : core.module(SmallItemsModule.class);
        if (m == null || stack == null || stack.isEmpty() || !m.affects(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())) {
            original.call(self, entity, stack, context, pose, buffers, light);
            return;
        }
        pose.pushPose();
        pose.translate(0, -m.lowerBy(), 0);
        float s = m.scale();
        pose.scale(s, s, s);
        original.call(self, entity, stack, context, pose, buffers, light);
        pose.popPose();
    }
}
