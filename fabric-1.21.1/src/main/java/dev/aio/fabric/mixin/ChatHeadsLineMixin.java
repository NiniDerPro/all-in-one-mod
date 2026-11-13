package dev.aio.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.aio.fabric.ChatHeads;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Chat Heads: indent lines of player messages when they are created. */
@Mixin(ChatComponent.class)
public abstract class ChatHeadsLineMixin {
    @WrapOperation(method = "addMessageToDisplayQueue", at = @At(value = "NEW",
            target = "(ILnet/minecraft/util/FormattedCharSequence;Lnet/minecraft/client/GuiMessageTag;Z)Lnet/minecraft/client/GuiMessage$Line;"))
    private GuiMessage.Line aio$chatHeadLine(int time, FormattedCharSequence content, GuiMessageTag tag, boolean end,
                                             Operation<GuiMessage.Line> original, @Local(argsOnly = true) GuiMessage message) {
        return original.call(time, ChatHeads.line(message, content), tag, end);
    }
}
