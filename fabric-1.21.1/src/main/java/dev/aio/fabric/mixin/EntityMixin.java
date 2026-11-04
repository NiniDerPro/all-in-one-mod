package dev.aio.fabric.mixin;

import dev.aio.core.AioCore;
import dev.aio.core.modules.render.FreelookModule;
import dev.aio.core.modules.render.ZoomModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mausbewegung des Spielers: Zoom verlangsamt sie, Freelook leitet sie auf die Kamera um. */
@Mixin(Entity.class)
public abstract class EntityMixin {
    private boolean aio$isLocalPlayer() {
        return (Object) this == MinecraftClient.getInstance().player;
    }

    @ModifyVariable(method = "changeLookDirection", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double aio$zoomSensitivityX(double dx) {
        return aio$isLocalPlayer() ? dx * aio$sensitivity() : dx;
    }

    @ModifyVariable(method = "changeLookDirection", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private double aio$zoomSensitivityY(double dy) {
        return aio$isLocalPlayer() ? dy * aio$sensitivity() : dy;
    }

    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void aio$freelook(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        AioCore core = AioCore.get();
        if (core == null || !aio$isLocalPlayer()) return;
        if (core.module(FreelookModule.class).consumeMouse(cursorDeltaX, cursorDeltaY)) ci.cancel();
    }

    private static double aio$sensitivity() {
        AioCore core = AioCore.get();
        return core == null ? 1 : core.module(ZoomModule.class).sensitivityMultiplier();
    }
}
