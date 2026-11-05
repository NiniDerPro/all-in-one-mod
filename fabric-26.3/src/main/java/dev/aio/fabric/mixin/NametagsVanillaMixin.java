package dev.aio.fabric.mixin;

import dev.aio.fabric.NametagTargets;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Nametags: Vanilla-Namensschild ausblenden, wo das Modul ein eigenes zeigt. */
@Mixin(EntityRenderer.class)
public abstract class NametagsVanillaMixin {
    @WrapOperation(method = "extractNameTags(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;FDD)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;shouldShowName(Lnet/minecraft/world/entity/Entity;D)Z"))
    private boolean aio$hideVanillaNametag(EntityRenderer<?, ?> renderer, Entity entity, double distanceSq, Operation<Boolean> original) {
        if (NametagTargets.hidesVanilla(entity)) return false;
        return original.call(renderer, entity, distanceSq);
    }
}
