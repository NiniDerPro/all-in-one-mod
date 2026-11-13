package dev.aio.forge.mixin;

import dev.aio.forge.FriendMarks;
import dev.aio.forge.FriendsTintHolder;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Friends: tint the whole skin in the rule colour ("Full"). */
@Mixin(LivingEntityRenderer.class)
public abstract class FriendsTintMixin {
    @Inject(method = "getModelTint", at = @At("RETURN"), cancellable = true)
    private void aio$friendTint(LivingEntityRenderState state, CallbackInfoReturnable<Integer> cir) {
        int tint = ((FriendsTintHolder) state).aio$friendTint();
        if (tint == 0) return;
        cir.setReturnValue(FriendMarks.multiply(cir.getReturnValueI(), tint));
    }
}
