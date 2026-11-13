package dev.aio.fabric.mixin;

import dev.aio.core.modules.render.FriendsModule;
import dev.aio.fabric.FriendMarks;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Friends: matching players get the glowing outline. */
@Mixin(Minecraft.class)
public abstract class FriendsGlowMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("RETURN"), cancellable = true)
    private void aio$friendGlow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) return;
        FriendsModule m = FriendMarks.module();
        if (m != null && m.outline() && FriendMarks.color(m, entity) != FriendsModule.NONE) cir.setReturnValue(true);
    }
}
