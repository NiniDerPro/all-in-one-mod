package dev.aio.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.aio.fabric.ShieldIndicator;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Shield Indicator: tints the shield model depending on the state of its holder. */
@Mixin(BlockEntityWithoutLevelRenderer.class)
public abstract class ShieldIndicatorRenderMixin {
    @Inject(method = "renderByItem", at = @At("HEAD"))
    private void aio$shieldBegin(CallbackInfo ci) {
        ShieldIndicator.current = ShieldIndicator.ownerTint();
    }

    @Inject(method = "renderByItem", at = @At("RETURN"))
    private void aio$shieldEnd(CallbackInfo ci) {
        ShieldIndicator.current = -1;
    }

    /** Colour of the shield parts (handle and plate); only the shield uses these calls here. */
    @WrapOperation(method = "renderByItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"))
    private void aio$shieldColor(ModelPart part, PoseStack pose, VertexConsumer consumer, int light, int overlay, Operation<Void> original) {
        if (ShieldIndicator.current != -1) { int c = ShieldIndicator.current; part.render(pose, consumer, light, overlay, ((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f, ((c >>> 24) & 255) / 255f); }
        else original.call(part, pose, consumer, light, overlay);
    }
}
