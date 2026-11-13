package dev.aio.neoforge.mixin;

import dev.aio.core.AioCore;
import dev.aio.core.event.Events;
import dev.aio.neoforge.NeoForgePlatform;
import dev.aio.neoforge.InputState;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At("HEAD"))
    private void aio$onKey(long window, int action, KeyEvent event, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (window != client.getWindow().handle()) return;
        String key = NeoForgePlatform.keyName(event);
        InputState.update(key, action);
        AioCore core = AioCore.get();
        if (core != null) core.bus().post(new Events.Key(key, action, client.screen != null));
    }
}
