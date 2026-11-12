package dev.aio.fabric.mixin;

import dev.aio.core.AioCore;
import dev.aio.core.event.Events;
import dev.aio.core.platform.Keys;
import dev.aio.fabric.FabricPlatform;
import dev.aio.fabric.InputState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method = "onPress", at = @At("HEAD"))
    private void aio$onButton(long window, int button, int action, int mods, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (window != client.getWindow().getWindow()) return;
        InputState.update(Keys.mouse(FabricPlatform.button(button)), action);
        AioCore core = AioCore.get();
        if (core != null) core.bus().post(new Events.MouseButton(FabricPlatform.button(button), action, client.screen != null));
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void aio$onScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        AioCore core = AioCore.get();
        if (core == null || window != client.getWindow().getWindow() || client.screen != null) return;
        if (core.bus().post(new Events.MouseScroll(vertical)).isCancelled()) ci.cancel();
    }
}
