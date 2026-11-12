package dev.aio.fabric.mixin;

import dev.aio.core.hud.HudEditorScreen;
import dev.aio.core.ui.ClickGuiScreen;
import dev.aio.fabric.AioScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Toasts lie at the top right exactly above search and buttons of the AIO menu: do not draw while it is open. */
@Mixin(ToastManager.class)
public abstract class ToastManagerMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void aio$hideOverMenu(GuiGraphics graphics, CallbackInfo ci) {
        if (Minecraft.getInstance().screen instanceof AioScreen s
                && (s.ui() instanceof ClickGuiScreen || s.ui() instanceof HudEditorScreen)) {
            ci.cancel();
        }
    }
}
