package dev.aio.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.aio.core.AioCore;
import dev.aio.core.modules.render.NoHurtcamModule;
import dev.aio.core.modules.render.NoViewBobbingModule;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;

/** No Hurtcam (the shake can be reduced instead of removed) and No View Bobbing, for the world and the hand. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @WrapMethod(method = "bobHurt", require = 0)
    private void aio$noHurtcam(PoseStack pose, float partialTicks, Operation<Void> original) {
        AioCore core = AioCore.get();
        double keep = core == null ? 1 : core.module(NoHurtcamModule.class).remaining();
        if (keep >= 1) {
            original.call(pose, partialTicks);
        } else if (keep > 0) {
            // Let the game compute the shake on a scratch stack, then apply only a part of that rotation
            PoseStack scratch = new PoseStack();
            original.call(scratch, partialTicks);
            Quaternionf shake = scratch.last().pose().getNormalizedRotation(new Quaternionf());
            pose.mulPose(new Quaternionf().slerp(shake, (float) keep));
        }
    }

    @WrapMethod(method = "bobView", require = 0)
    private void aio$noViewBobbing(PoseStack pose, float partialTicks, Operation<Void> original) {
        AioCore core = AioCore.get();
        if (core != null && core.module(NoViewBobbingModule.class).suppress()) return;
        original.call(pose, partialTicks);
    }
}
