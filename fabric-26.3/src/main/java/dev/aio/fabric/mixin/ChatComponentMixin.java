package dev.aio.fabric.mixin;

import dev.aio.core.AioCore;
import dev.aio.core.modules.chat.ChatModule;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Chat: Zeitstempel und längerer Verlauf (Vanilla: 100 Zeilen).
 * Unverifiziert für 26.3, darum require = 0: fehlt ein Ziel, bleibt nur die Funktion weg.
 */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at = @At("HEAD"), argsOnly = true, require = 0)
    private Component aio$timestamp(Component message) {
        AioCore core = AioCore.get();
        if (core == null) return message;
        String prefix = core.module(ChatModule.class).timestampPrefix();
        if (prefix == null) return message;
        return Component.literal(prefix).withStyle(ChatFormatting.GRAY).append(message);
    }

    @ModifyConstant(method = {"addMessageToQueue", "addMessageToDisplayQueue"},
            constant = @Constant(intValue = 100), require = 0)
    private int aio$historySize(int original) {
        AioCore core = AioCore.get();
        return core == null ? original : core.module(ChatModule.class).historySize();
    }
}
