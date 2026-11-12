package dev.aio.fabric.mixin;

import dev.aio.fabric.Freecam;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Adapted from Freecam (MIT), see dev/aio/fabric/freecam/NOTICE.md
@Mixin(ClientPacketListener.class)
public class FreecamPacketListenerMixin {
    /** Respawn oder Dimensionswechsel: ausschalten. */
    @Inject(method = "handleRespawn", at = @At("TAIL"))
    private void aio$freecamRespawn(CallbackInfo ci) {
        Freecam.set(false);
    }
}
