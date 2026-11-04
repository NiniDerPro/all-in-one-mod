package dev.aio.fabric.mixin;

import dev.aio.core.AioCore;
import dev.aio.core.modules.render.NoPumpkinModule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No Pumpkin Overlay: überspringt nur das Kürbis-Bild, andere Overlays (Pulverschnee, ...) bleiben. */
@Mixin(Hud.class)
public abstract class HudMixin {
    @Inject(method = "extractTextureOverlay", at = @At("HEAD"), cancellable = true)
    private void aio$noPumpkin(GuiGraphicsExtractor graphics, Identifier texture, float alpha, CallbackInfo ci) {
        AioCore core = AioCore.get();
        if (core != null && core.module(NoPumpkinModule.class).suppress() && texture.getPath().contains("pumpkinblur")) {
            ci.cancel();
        }
    }
}
