package dev.aio.forge.mixin;

import dev.aio.forge.NametagTargets;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Nametags: hide the vanilla nametag where the module shows its own. */
@Mixin(EntityRenderer.class)
public abstract class NametagsVanillaMixin {
    @WrapOperation(method = "extractNameTags(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;FDD)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;shouldShowName(Lnet/minecraft/world/entity/Entity;D)Z"))
    private boolean aio$hideVanillaNametag(EntityRenderer<?, ?> renderer, Entity entity, double distanceSq, Operation<Boolean> original) {
        if (NametagTargets.hidesVanilla(entity)) return false;
        return original.call(renderer, entity, distanceSq);
    }
}
