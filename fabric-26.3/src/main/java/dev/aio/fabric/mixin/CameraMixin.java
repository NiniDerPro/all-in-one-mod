package dev.aio.fabric.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.aio.core.AioCore;
import dev.aio.core.modules.render.FreelookModule;
import dev.aio.core.modules.render.ZoomModule;
import dev.aio.fabric.FabricHooks;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Kamera: Zoom verkleinert das Sichtfeld der Welt (nicht das der Hand), Freelook ersetzt
 * die Blickrichtung des Spielers, aus der die Kamera ihre Rotation berechnet.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @ModifyReturnValue(method = "calculateFov", at = @At("RETURN"))
    private float aio$zoom(float fov) {
        AioCore core = AioCore.get();
        float result = core == null ? fov : (float) (fov * core.module(ZoomModule.class).fovMultiplier());
        FabricHooks.lastFov = result; // echtes FOV für die Wegpunkt-Marker
        return result;
    }

    // Unverifiziert für 26.3: Falls Camera die Rotation nicht mehr über getViewYRot/getViewXRot liest,
    // greift das nicht (require = 0 verhindert dann einen Absturz, Freelook dreht dann nur den Spieler nicht).
    @WrapOperation(method = "*", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F"))
    private float aio$freelookYaw(Entity entity, float partialTick, Operation<Float> original) {
        FabricHooks.cameraHookSeen = true; // Hook greift: Freelook darf die Maus übernehmen
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
