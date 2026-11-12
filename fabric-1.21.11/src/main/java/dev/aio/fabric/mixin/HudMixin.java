package dev.aio.fabric.mixin;

import dev.aio.core.AioCore;
import dev.aio.core.modules.render.NoPumpkinModule;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Gui;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No Pumpkin Overlay: only skips the pumpkin picture, other overlays (powder snow, ...) stay. */
@Mixin(Gui.class)
public abstract class HudMixin {
    @Inject(method = "extractTextureOverlay", at = @At("HEAD"), cancellable = true)
    private void aio$noPumpkin(GuiGraphics graphics, Identifier texture, float alpha, CallbackInfo ci) {
        AioCore core = AioCore.get();
        if (core != null && core.module(NoPumpkinModule.class).suppress() && texture.getPath().contains("pumpkinblur")) {
            ci.cancel();
        }
    }
}
