package dev.aio.forge.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.aio.core.AioCore;
import dev.aio.core.modules.render.NoSpeedFovModule;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** No Speed FOV: sprinting, flying and speed effects change the field of view less (or not at all). */
@Mixin(AbstractClientPlayer.class)
public abstract class SpeedFovMixin {
    @ModifyReturnValue(method = "getFieldOfViewModifier", at = @At("RETURN"), require = 0)
    private float aio$speedFov(float modifier) {
        AioCore core = AioCore.get();
        if (core == null || ((AbstractClientPlayer) (Object) this).isUsingItem()) return modifier; // bow and spyglass stay
        double keep = core.module(NoSpeedFovModule.class).remaining();
        return keep >= 1 ? modifier : (float) (1 + (modifier - 1) * keep);
    }
}
