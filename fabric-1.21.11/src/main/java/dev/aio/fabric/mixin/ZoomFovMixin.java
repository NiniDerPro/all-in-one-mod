package dev.aio.fabric.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.aio.core.AioCore;
import dev.aio.core.modules.render.ZoomModule;
import dev.aio.fabric.FabricHooks;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Zoom: shrinks the field of view of the world; the hand (drawn with its own call, "useFovSetting" off) is
 * scaled by the current factor so that it moves out of the picture.
 */
@Mixin(GameRenderer.class)
public abstract class ZoomFovMixin {
    @ModifyReturnValue(method = "getFov", at = @At("RETURN"))
    private float aio$zoomFov(float fov, Camera camera, float partialTick, boolean useFovSetting) {
        AioCore core = AioCore.get();
        if (core == null) return fov;
        ZoomModule zoom = core.module(ZoomModule.class);
        if (!useFovSetting) return (float) (fov * zoom.currentMultiplier());
        float result = (float) (fov * zoom.fovMultiplier());
        FabricHooks.lastFov = (float) result; // real FOV for the waypoint markers
        return result;
    }
}
