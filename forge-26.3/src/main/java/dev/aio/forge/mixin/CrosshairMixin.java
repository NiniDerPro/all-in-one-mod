package dev.aio.forge.mixin;

import dev.aio.core.AioCore;
import dev.aio.core.modules.render.CrosshairModule;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Crosshair: hide the vanilla crosshair while the own one is drawn. */
@Mixin(Hud.class)
public abstract class CrosshairMixin {
    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void aio$crosshair(CallbackInfo ci) {
        AioCore core = AioCore.get();
        CrosshairModule m = core == null ? null : core.module(CrosshairModule.class);
        if (m != null && m.replacesVanilla()) ci.cancel();
    }
}
