package dev.aio.neoforge.mixin;

import dev.aio.neoforge.FriendsTintHolder;

import dev.aio.core.modules.render.FriendsModule;
import dev.aio.neoforge.FriendMarks;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Friends: outline in the rule colour, or remember the tint for the model. */
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
        if (m.outline()) state.outlineColor = color | 0xFF000000;
        if (m.fill()) holder.aio$setFriendTint(m.tintFor(color));
    }
}
