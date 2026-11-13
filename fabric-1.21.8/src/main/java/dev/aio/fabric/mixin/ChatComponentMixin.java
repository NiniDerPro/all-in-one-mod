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
 * Chat: timestamps and longer history (vanilla: 100 lines).
 * Unverified for 26.3, hence require = 0: if a target is missing, only the feature is lost.
 */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            at = @At("HEAD"), argsOnly = true, require = 0)
    private Component aio$timestamp(Component message) {
        AioCore core = AioCore.get();
        if (core == null) return message;
        message = dev.aio.fabric.ChatWaypointsBridge.rewrite(core, message);
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
