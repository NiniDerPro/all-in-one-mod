package dev.aio.fabric.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.aio.fabric.ShieldIndicator;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.ShieldSpecialRenderer;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Shield Indicator: tints the shield model depending on the state. */
@Mixin(ShieldSpecialRenderer.class)
public abstract class ShieldIndicatorRenderMixin {
    @ModifyReturnValue(method = "extractArgument(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/core/component/DataComponentMap;",
            at = @At("RETURN"))
    private DataComponentMap aio$shieldTag(DataComponentMap map, ItemStack stack) {
        return ShieldIndicator.tag(map);
    }

    @Inject(method = "submit(Lnet/minecraft/core/component/DataComponentMap;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IIZI)V",
            at = @At("HEAD"))
    private void aio$shieldBegin(DataComponentMap map, PoseStack pose, SubmitNodeCollector collector, int light, int overlay,
                                 boolean glint, int outline, CallbackInfo ci) {
        ShieldIndicator.current = ShieldIndicator.tintOf(map);
    }

    @Inject(method = "submit(Lnet/minecraft/core/component/DataComponentMap;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IIZI)V",
            at = @At("RETURN"))
    private void aio$shieldEnd(DataComponentMap map, PoseStack pose, SubmitNodeCollector collector, int light, int overlay,
                               boolean glint, int outline, CallbackInfo ci) {
        ShieldIndicator.current = -1;
    }

    /** Colour of the base model (with banner pattern). */
    @ModifyArg(method = "submit(Lnet/minecraft/core/component/DataComponentMap;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IIZI)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/UvMapping;I)V"),
            index = 6)
    private int aio$shieldColorA(int color) {
        return ShieldIndicator.current != -1 ? ShieldIndicator.current : color;
    }

    /** Colour of the base model (without pattern). */
    @ModifyArg(method = "submit(Lnet/minecraft/core/component/DataComponentMap;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IIZI)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;IIILnet/minecraft/client/resources/model/sprite/SpriteId;Lnet/minecraft/client/resources/model/sprite/SpriteGetter;I)V"),
            index = 5)
    private int aio$shieldColorB(int color) {
        return ShieldIndicator.current != -1 ? ShieldIndicator.current : color;
    }
}
