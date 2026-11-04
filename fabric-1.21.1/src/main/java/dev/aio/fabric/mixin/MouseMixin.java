package dev.aio.fabric.mixin;

import dev.aio.core.AioCore;
import dev.aio.core.event.Events;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void aio$onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        AioCore core = AioCore.get();
        if (core == null || window != client.getWindow().getHandle()) return;
        core.bus().post(new Events.MouseButton(button, action, client.currentScreen != null));
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void aio$onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        AioCore core = AioCore.get();
        if (core == null || window != client.getWindow().getHandle() || client.currentScreen != null) return;
        if (core.bus().post(new Events.MouseScroll(vertical)).isCancelled()) ci.cancel();
    }
}
