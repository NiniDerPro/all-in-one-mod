package dev.aio.fabric.mixin;

import dev.aio.fabric.FriendMarks;
import dev.aio.fabric.FriendsTintHolder;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Friends: remember the tint while the armour of a player is drawn. */
@Mixin(HumanoidArmorLayer.class)
public abstract class FriendsArmorLayerMixin {
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V", at = @At("HEAD"))
    private void aio$start(PoseStack pose, MultiBufferSource buffers, int light, HumanoidRenderState state, float yaw, float pitch, CallbackInfo ci) {
        FriendMarks.current = ((FriendsTintHolder) state).aio$friendTint();
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V", at = @At("RETURN"))
    private void aio$end(PoseStack pose, MultiBufferSource buffers, int light, HumanoidRenderState state, float yaw, float pitch, CallbackInfo ci) {
        FriendMarks.current = 0;
    }
}
