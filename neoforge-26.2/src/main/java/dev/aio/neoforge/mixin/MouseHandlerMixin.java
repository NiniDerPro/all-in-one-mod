package dev.aio.neoforge.mixin;

import dev.aio.core.AioCore;
import dev.aio.core.event.Events;
import dev.aio.core.platform.Keys;
import dev.aio.neoforge.NeoForgePlatform;
import dev.aio.neoforge.InputState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method = "onButton", at = @At("HEAD"))
    private void aio$onButton(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (window != client.getWindow().handle()) return;
        InputState.update(Keys.mouse(NeoForgePlatform.button(info.button())), action);
        AioCore core = AioCore.get();
        if (core != null) core.bus().post(new Events.MouseButton(NeoForgePlatform.button(info.button()), action, client.gui.screen() != null));
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void aio$onScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        AioCore core = AioCore.get();
        if (core == null || window != client.getWindow().handle() || client.gui.screen() != null) return;
        if (core.bus().post(new Events.MouseScroll(vertical)).isCancelled()) ci.cancel();
    }
}
