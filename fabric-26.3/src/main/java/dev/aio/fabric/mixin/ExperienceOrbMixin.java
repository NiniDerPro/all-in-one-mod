package dev.aio.fabric.mixin;

import dev.aio.core.AioCore;
import dev.aio.core.modules.performance.XpClumpsModule;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** XP sofort sammeln: keine Pause zwischen zwei Kugeln (nur auf dem eigenen Einzelspieler-Server). */
@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMixin {
    @Inject(method = "playerTouch", at = @At("HEAD"), require = 0)
    private void aio$instantPickup(Player player, CallbackInfo ci) {
        if (player.level().isClientSide()) return;
        AioCore core = AioCore.get();
        XpClumpsModule m = core == null ? null : core.module(XpClumpsModule.class);
        if (m != null && m.isActive()) player.takeXpDelay = 0;
    }
}
