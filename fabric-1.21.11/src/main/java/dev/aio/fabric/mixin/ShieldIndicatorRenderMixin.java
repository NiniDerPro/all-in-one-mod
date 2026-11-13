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

    @Inject(method = "submit(Lnet/minecraft/core/component/DataComponentMap;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IIZI)V",
            at = @At("HEAD"))
    private void aio$shieldBegin(DataComponentMap map, net.minecraft.world.item.ItemDisplayContext context, PoseStack pose, SubmitNodeCollector collector, int light, int overlay,
                                 boolean glint, int outline, CallbackInfo ci) {
        ShieldIndicator.current = ShieldIndicator.tintOf(map);
    }

    @Inject(method = "submit(Lnet/minecraft/core/component/DataComponentMap;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IIZI)V",
            at = @At("RETURN"))
    private void aio$shieldEnd(DataComponentMap map, net.minecraft.world.item.ItemDisplayContext context, PoseStack pose, SubmitNodeCollector collector, int light, int overlay,
                               boolean glint, int outline, CallbackInfo ci) {
        ShieldIndicator.current = -1;
    }

    /** Colour of the shield parts (plate and handle). */
    @ModifyArg(method = "submit(Lnet/minecraft/core/component/DataComponentMap;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IIZI)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModelPart(Lnet/minecraft/client/model/geom/ModelPart;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ZZILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;I)V"),
            index = 8)
    private int aio$shieldColor(int color) {
        return ShieldIndicator.current != -1 ? ShieldIndicator.current : color;
    }
}
