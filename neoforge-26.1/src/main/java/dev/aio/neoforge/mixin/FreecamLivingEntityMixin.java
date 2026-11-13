package dev.aio.neoforge.mixin;

import dev.aio.neoforge.Freecam;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Adapted from Freecam (MIT), see dev/aio/fabric/freecam/NOTICE.md
@Mixin(LivingEntity.class)
public abstract class FreecamLivingEntityMixin {
    @Shadow public abstract float getHealth();

    /** Bei Schaden ausschalten (Einstellung "Turn off when taking damage"). */
    @Inject(method = "setHealth", at = @At("HEAD"))
    private void aio$freecamDamage(float health, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (Freecam.active() && Freecam.disableOnDamage() && (Object) this == mc.player
                && !mc.player.isCreative() && getHealth() > health) {
            Freecam.disableNextTick();
        }
    }
}
