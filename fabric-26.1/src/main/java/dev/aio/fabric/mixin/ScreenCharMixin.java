package dev.aio.fabric.mixin;

import dev.aio.fabric.ScreenInput;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Typed characters on screens (search field of the item list): lets the adapter take them first. */
@Mixin(KeyboardHandler.class)
public abstract class ScreenCharMixin {
    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true, require = 0)
    private void aio$screenChar(long window, CharacterEvent event, CallbackInfo ci) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen != null && !ScreenInput.allowChar(screen, event.codepoint())) ci.cancel();
    }
}
