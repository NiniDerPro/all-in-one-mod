package dev.aio.fabric.mixin;

import dev.aio.fabric.FriendsTintHolder;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Friends: room for the model tint on every render state. */
@Mixin(EntityRenderState.class)
public abstract class FriendsRenderStateMixin implements FriendsTintHolder {
    @Unique private int aio$tint;

    @Override public int aio$friendTint() { return aio$tint; }

    @Override public void aio$setFriendTint(int tint) { aio$tint = tint; }
}
