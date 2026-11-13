package dev.aio.neoforge.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.aio.core.AioCore;
import dev.aio.core.modules.render.FreelookModule;
import dev.aio.core.modules.render.ZoomModule;
import dev.aio.neoforge.NeoForgeHooks;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Camera: zoom shrinks the field of view of the world (not that of the hand), freelook replaces
 * the player's viewing direction from which the camera computes its rotation.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @ModifyReturnValue(method = "calculateFov", at = @At("RETURN"))
    private float aio$zoom(float fov) {
        AioCore core = AioCore.get();
        float result = core == null ? fov : (float) (fov * core.module(ZoomModule.class).fovMultiplier());
        NeoForgeHooks.lastFov = result; // real FOV for the waypoint markers
        return result;
    }

    /** The hand moves out of the picture when zooming (like the field of view). */
    @ModifyReturnValue(method = "calculateHudFov", at = @At("RETURN"), require = 0)
    private float aio$zoomHand(float fov) {
        AioCore core = AioCore.get();
        return core == null ? fov : (float) (fov * core.module(ZoomModule.class).currentMultiplier());
    }

    // Unverified for 26.3: if Camera no longer reads the rotation via getViewYRot/getViewXRot,
    // this does not apply (require = 0 then prevents a crash, freelook then just does not turn the player).
    @WrapOperation(method = "*", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F"))
    private float aio$freelookYaw(Entity entity, float partialTick, Operation<Float> original) {
        NeoForgeHooks.cameraHookSeen = true; // hook applies: freelook may take over the mouse
        FreelookModule f = aio$freelook(entity);
        return f != null ? f.cameraYaw() : original.call(entity, partialTick);
    }

    @WrapOperation(method = "*", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F"))
    private float aio$freelookPitch(Entity entity, float partialTick, Operation<Float> original) {
        FreelookModule f = aio$freelook(entity);
        return f != null ? f.cameraPitch() : original.call(entity, partialTick);
    }

    @Unique
    private static FreelookModule aio$freelook(Entity entity) {
        AioCore core = AioCore.get();
        if (core == null || entity != Minecraft.getInstance().player) return null;
        FreelookModule f = core.module(FreelookModule.class);
        return f.isEngaged() ? f : null;
    }
}
