package dev.aio.fabric.mixin;

import dev.aio.fabric.FriendMarks;

import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Friends: tint the armour too, so the whole player is filled in the rule colour. */
@Mixin(EquipmentLayerRenderer.class)
public abstract class FriendsArmorMixin {
    @ModifyArg(method = "renderLayers*", index = 4,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/Model;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"))
    private int aio$friendArmour(int color) {
        return FriendMarks.current == 0 ? color : FriendMarks.multiply(color, FriendMarks.current);
    }
}
