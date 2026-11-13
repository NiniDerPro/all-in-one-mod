package dev.aio.fabric.mixin;

import dev.aio.fabric.FriendsTintHolder;

import dev.aio.core.modules.render.FriendsModule;
import dev.aio.fabric.FriendMarks;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Friends: remember the tint for the model (the outline comes from the glow mixins). */
@Mixin(EntityRenderer.class)
public abstract class FriendsExtractMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V",
            at = @At("TAIL"))
    private void aio$friends(Entity entity, EntityRenderState state, float partialTick, CallbackInfo ci) {
        FriendsTintHolder holder = (FriendsTintHolder) state;
        holder.aio$setFriendTint(0);
        FriendsModule m = FriendMarks.module();
        if (m == null) return;
        int color = FriendMarks.color(m, entity);
        if (color == FriendsModule.NONE) return;
        if (m.fill()) holder.aio$setFriendTint(m.tintFor(color));
    }
}
