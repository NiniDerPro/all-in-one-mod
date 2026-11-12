package dev.aio.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.aio.fabric.ChatHeads;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Chat Heads: indent lines of player messages when they are created. */
@Mixin(ChatComponent.class)
public abstract class ChatHeadsLineMixin {
    @WrapOperation(method = "addMessageToDisplayQueue", at = @At(value = "NEW",
            target = "(Lnet/minecraft/client/multiplayer/chat/GuiMessage;Lnet/minecraft/util/FormattedCharSequence;Z)Lnet/minecraft/client/multiplayer/chat/GuiMessage$Line;"))
    private GuiMessage.Line aio$chatHeadLine(GuiMessage message, FormattedCharSequence content, boolean end, Operation<GuiMessage.Line> original) {
        return original.call(message, ChatHeads.line(message, content), end);
    }
}
