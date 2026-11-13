package dev.aio.fabric.mixin;

import dev.aio.core.modules.render.FriendsModule;
import dev.aio.fabric.FriendMarks;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Friends: the outline takes the rule colour. */
@Mixin(Entity.class)
public abstract class FriendsTeamColorMixin {
    @Inject(method = "getTeamColor", at = @At("RETURN"), cancellable = true)
    private void aio$friendColor(CallbackInfoReturnable<Integer> cir) {
        FriendsModule m = FriendMarks.module();
        if (m == null || !m.outline()) return;
        int color = FriendMarks.color(m, (Entity) (Object) this);
        if (color != FriendsModule.NONE) cir.setReturnValue(color & 0xFFFFFF);
    }
}
