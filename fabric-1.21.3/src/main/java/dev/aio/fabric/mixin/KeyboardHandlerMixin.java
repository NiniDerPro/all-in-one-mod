package dev.aio.fabric.mixin;

import dev.aio.core.AioCore;
import dev.aio.core.event.Events;
import dev.aio.fabric.FabricPlatform;
import dev.aio.fabric.InputState;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At("HEAD"))
    private void aio$onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (window != client.getWindow().getWindow()) return;
        String keyName = FabricPlatform.keyName(key, scancode);
        InputState.update(keyName, action);
        AioCore core = AioCore.get();
        if (core != null) core.bus().post(new Events.Key(keyName, action, client.screen != null));
    }
}
