package dev.aio.fabric.mixin;

import dev.aio.core.hud.HudEditorScreen;
import dev.aio.core.ui.ClickGuiScreen;
import dev.aio.fabric.AioScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Toasts liegen oben rechts genau über Suche und Knöpfen des AIO-Menüs: solange es offen ist, nicht zeichnen. */
@Mixin(ToastManager.class)
public abstract class ToastManagerMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void aio$hideOverMenu(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (Minecraft.getInstance().gui.screen() instanceof AioScreen s
                && (s.ui() instanceof ClickGuiScreen || s.ui() instanceof HudEditorScreen)) {
            ci.cancel();
        }
    }
}
