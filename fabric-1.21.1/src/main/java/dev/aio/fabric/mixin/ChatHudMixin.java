package dev.aio.fabric.mixin;

import dev.aio.core.AioCore;
import dev.aio.core.modules.chat.ChatModule;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Chat: Zeitstempel und längerer Verlauf (Vanilla: 100 Zeilen). */
@Mixin(ChatHud.class)
public abstract class ChatHudMixin {
    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"), argsOnly = true)
    private Text aio$timestamp(Text message) {
        AioCore core = AioCore.get();
        if (core == null) return message;
        String prefix = core.module(ChatModule.class).timestampPrefix();
        if (prefix == null) return message;
        return Text.literal(prefix).formatted(Formatting.GRAY).append(message);
    }

    @ModifyConstant(
            method = {"addMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V", "addVisibleMessage"},
            constant = @Constant(intValue = 100))
    private int aio$historySize(int original) {
        AioCore core = AioCore.get();
        return core == null ? original : core.module(ChatModule.class).historySize();
    }
}
