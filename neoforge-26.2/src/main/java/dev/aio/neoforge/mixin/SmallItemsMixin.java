package dev.aio.neoforge.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.aio.core.AioCore;
import dev.aio.core.modules.render.SmallItemsModule;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Small Shield & Totem: draw the item in hand smaller and a bit lower. */
@Mixin(ItemInHandRenderer.class)
public abstract class SmallItemsMixin {
    @WrapOperation(method = "submitArmWithItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
    private void aio$smallItems(ItemStackRenderState state, PoseStack pose, SubmitNodeCollector collector, int a, int b, int c,
                                Operation<Void> original, @Local(argsOnly = true) ItemStack stack) {
        AioCore core = AioCore.get();
        SmallItemsModule m = core == null ? null : core.module(SmallItemsModule.class);
        if (m == null || stack == null || stack.isEmpty() || !m.affects(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())) {
            original.call(state, pose, collector, a, b, c);
            return;
        }
        pose.pushPose();
        pose.translate(0, -m.lowerBy(), 0);
        float s = m.scale();
        pose.scale(s, s, s);
        original.call(state, pose, collector, a, b, c);
        pose.popPose();
    }
}
