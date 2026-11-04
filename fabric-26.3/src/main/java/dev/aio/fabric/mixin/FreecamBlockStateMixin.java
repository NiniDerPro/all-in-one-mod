package dev.aio.fabric.mixin;

import dev.aio.fabric.Freecam;
import dev.aio.fabric.freecam.FreeCamera;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Aus Freecam (MIT) übernommen, siehe dev/aio/fabric/freecam/NOTICE.md
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class FreecamBlockStateMixin {
    /** Durch Blöcke fliegen: für die Kamera haben Blöcke keine Kollision. */
    @Inject(method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("HEAD"), cancellable = true)
    private void aio$freecamNoClip(BlockGetter level, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        if (context instanceof EntityCollisionContext ec && ec.getEntity() instanceof FreeCamera && Freecam.active() && Freecam.noClip()) {
            cir.setReturnValue(Shapes.empty());
        }
    }
}
