package dev.aio.fabric.mixin;

import dev.aio.core.AioCore;
import dev.aio.core.event.Events;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public abstract class KeyboardMixin {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "onKey", at = @At("HEAD"))
    private void aio$onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        AioCore core = AioCore.get();
        if (core == null || window != client.getWindow().getHandle()) return;
        core.bus().post(new Events.Key(key, action, modifiers, client.currentScreen != null));
    }
}
