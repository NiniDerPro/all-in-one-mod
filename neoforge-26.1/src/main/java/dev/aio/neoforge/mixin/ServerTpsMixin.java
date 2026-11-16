package dev.aio.neoforge.mixin;

import dev.aio.core.util.ServerTps;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Time updates arrive every 20 server ticks: their spacing gives the server TPS (Info Lines). */
@Mixin(ClientPacketListener.class)
public abstract class ServerTpsMixin {
    @Inject(method = "handleSetTime", at = @At("TAIL"))
    private void aio$timeUpdate(CallbackInfo ci) {
        ServerTps.onTimePacket();
    }
}
