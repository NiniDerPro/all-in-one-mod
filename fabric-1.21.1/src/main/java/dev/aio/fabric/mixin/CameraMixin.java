package dev.aio.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.aio.core.AioCore;
import dev.aio.core.modules.render.FreelookModule;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Freelook: Die Kamera nutzt die eigene Yaw/Pitch statt der des Spielers. */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @WrapOperation(method = "update",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V", ordinal = 0))
    private void aio$freelookRotation(Camera camera, float yaw, float pitch, Operation<Void> original) {
        AioCore core = AioCore.get();
        FreelookModule freelook = core == null ? null : core.module(FreelookModule.class);
        if (freelook != null && freelook.isEngaged()) {
            original.call(camera, freelook.cameraYaw(), freelook.cameraPitch());
        } else {
            original.call(camera, yaw, pitch);
        }
    }
}
