package dev.aio.fabric.mixin;

import dev.aio.fabric.ChatHeads;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Chat Heads: Kopf vor die Zeile zeichnen, im normalen und im geöffneten Chat. */
@Mixin(targets = {
        "net.minecraft.client.gui.components.ChatComponent$DrawingBackgroundGraphicsAccess",
        "net.minecraft.client.gui.components.ChatComponent$DrawingFocusedGraphicsAccess"}, remap = false)
public abstract class ChatHeadsDrawMixin {
    @Shadow(remap = false) @Final private GuiGraphicsExtractor graphics;

    @Inject(method = "handleMessage", at = @At("HEAD"), remap = false)
    private void aio$chatHead(int y, float alpha, FormattedCharSequence content, CallbackInfoReturnable<Boolean> cir) {
        ChatHeads.draw(graphics, y, alpha, content);
    }
}
