package dev.aio.fabric.mixin;

import dev.aio.core.AioCore;
import dev.aio.core.modules.render.FreelookModule;
import dev.aio.core.modules.render.ZoomModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mausbewegung des Spielers ({@code Entity.turn}): Zoom verlangsamt sie, Freelook leitet sie auf die Kamera um. */
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Unique
    private boolean aio$isLocalPlayer() {
        return (Object) this == Minecraft.getInstance().player;
    }

    @ModifyVariable(method = "turn", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double aio$zoomSensitivityX(double dx) {
        return aio$isLocalPlayer() ? dx * aio$sensitivity() : dx;
    }

    @ModifyVariable(method = "turn", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private double aio$zoomSensitivityY(double dy) {
        return aio$isLocalPlayer() ? dy * aio$sensitivity() : dy;
    }

    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void aio$freelook(double dx, double dy, CallbackInfo ci) {
        AioCore core = AioCore.get();
        if (core == null || !aio$isLocalPlayer()) return;
        if (dev.aio.fabric.Freecam.turn(dx, dy)) { // Freecam: Maus dreht die Kamera
            ci.cancel();
            return;
        }
        if (core.module(FreelookModule.class).consumeMouse(dx, dy)) ci.cancel();
    }

    @Unique
    private static double aio$sensitivity() {
        AioCore core = AioCore.get();
        return core == null ? 1 : core.module(ZoomModule.class).sensitivityMultiplier();
    }
}
