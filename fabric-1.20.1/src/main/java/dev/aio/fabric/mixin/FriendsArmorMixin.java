package dev.aio.fabric.mixin;

import dev.aio.fabric.FriendMarks;

import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Friends: tint the armour too, so the whole player is filled in the rule colour. */
@Mixin(HumanoidArmorLayer.class)
public abstract class FriendsArmorMixin {
    private static final String MODEL = "Lnet/minecraft/client/model/HumanoidModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V";

    @ModifyArg(method = "renderModel*", index = 4, at = @At(value = "INVOKE", target = MODEL))
    private float aio$red(float v) { return FriendMarks.channel(v, 16); }

    @ModifyArg(method = "renderModel*", index = 5, at = @At(value = "INVOKE", target = MODEL))
    private float aio$green(float v) { return FriendMarks.channel(v, 8); }

    @ModifyArg(method = "renderModel*", index = 6, at = @At(value = "INVOKE", target = MODEL))
    private float aio$blue(float v) { return FriendMarks.channel(v, 0); }
}
