package dev.aio.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.aio.fabric.ChatHeads;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Chat Heads: draw the head in front of the line. In this Minecraft version the chat draws each line with one
 * text call inside the chat component (in a generated helper method, hence the wildcard).
 */
@Mixin(ChatComponent.class)
public abstract class ChatHeadsDrawMixin {
    @WrapOperation(method = "*", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)V"))
    private void aio$chatHead(GuiGraphics graphics, Font font, FormattedCharSequence content, int x, int y, int color, Operation<Void> original) {
        ChatHeads.draw(graphics, y, (color >>> 24) / 255f, content);
        original.call(graphics, font, content, x, y, color);
    }
}
