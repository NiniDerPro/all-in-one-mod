package dev.aio.fabric.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.aio.fabric.ShieldIndicator;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.special.ShieldSpecialRenderer;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Shield Indicator: tints the shield model depending on the state. */
@Mixin(ShieldSpecialRenderer.class)
public abstract class ShieldIndicatorRenderMixin {
    private static final String RENDER = "render(Lnet/minecraft/core/component/DataComponentMap;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IIZ)V";

    @ModifyReturnValue(method = "extractArgument(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/core/component/DataComponentMap;",
            at = @At("RETURN"))
    private DataComponentMap aio$shieldTag(DataComponentMap map, ItemStack stack) {
        return ShieldIndicator.tag(map);
    }

    @Inject(method = RENDER, at = @At("HEAD"))
    private void aio$shieldBegin(DataComponentMap map, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay,
                                 boolean glint, CallbackInfo ci) {
        ShieldIndicator.current = ShieldIndicator.tintOf(map);
    }

    @Inject(method = RENDER, at = @At("RETURN"))
    private void aio$shieldEnd(DataComponentMap map, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay,
                               boolean glint, CallbackInfo ci) {
        ShieldIndicator.current = -1;
    }

    /** Colour of the shield parts (handle and plate). */
    @WrapOperation(method = RENDER, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"))
    private void aio$shieldColor(ModelPart part, PoseStack pose, VertexConsumer consumer, int light, int overlay, Operation<Void> original) {
        if (ShieldIndicator.current != -1) part.render(pose, consumer, light, overlay, ShieldIndicator.current);
        else original.call(part, pose, consumer, light, overlay);
    }
}
