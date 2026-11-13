package dev.aio.fabric.mixin;

import dev.aio.core.modules.render.FriendsModule;
import dev.aio.fabric.FriendMarks;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Friends: tint the whole skin in the rule colour while the player is drawn ("Full"). */
@Mixin(LivingEntityRenderer.class)
public abstract class FriendsLivingMixin {
    private static final String RENDER = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V";

    @Inject(method = RENDER, at = @At("HEAD"))
    private void aio$start(LivingEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        FriendMarks.current = 0;
        FriendsModule m = FriendMarks.module();
        if (m == null || !m.fill()) return;
        int color = FriendMarks.color(m, entity);
        if (color != FriendsModule.NONE) FriendMarks.current = m.tintFor(color);
    }

    @Inject(method = RENDER, at = @At("RETURN"))
    private void aio$end(LivingEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        FriendMarks.current = 0;
    }

    @ModifyArg(method = RENDER, index = 4,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"))
    private int aio$tint(int color) {
        return FriendMarks.current == 0 ? color : FriendMarks.multiply(color, FriendMarks.current);
    }
}
