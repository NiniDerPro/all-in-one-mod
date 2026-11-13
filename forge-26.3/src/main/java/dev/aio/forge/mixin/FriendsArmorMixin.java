package dev.aio.forge.mixin;

import dev.aio.forge.FriendMarks;
import dev.aio.forge.FriendsTintHolder;

import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Friends: tint the armour too, so the whole player is filled in the rule colour. */
@Mixin(EquipmentLayerRenderer.class)
public abstract class FriendsArmorMixin {
    /** submitModel(model, state, pose, renderType, light, overlay, colour, ...): the render state is argument 1, the colour argument 6. */
    @ModifyArgs(method = "renderLayers*",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel"))
    private void aio$friendArmour(Args args) {
        if (args.get(1) instanceof FriendsTintHolder h && h.aio$friendTint() != 0) {
            args.set(6, FriendMarks.multiply(args.<Integer>get(6), h.aio$friendTint()));
        }
    }
}
