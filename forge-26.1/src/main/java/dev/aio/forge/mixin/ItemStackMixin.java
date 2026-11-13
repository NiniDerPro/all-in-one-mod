package dev.aio.forge.mixin;

import dev.aio.core.AioCore;
import dev.aio.core.modules.inventory.ShulkerPreviewModule;
import dev.aio.core.modules.player.FoodInfoModule;
import dev.aio.forge.FoodTooltip;
import dev.aio.forge.ShulkerTooltipHook;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/** Shulker preview: delivers the grid as a tooltip image. */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method = "getTooltipImage()Ljava/util/Optional;", at = @At("HEAD"), cancellable = true)
    private void aio$tooltipImage(CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
        AioCore core = AioCore.get();
        if (core == null) return;
        ItemStack stack = (ItemStack) (Object) this;
        ShulkerPreviewModule shulker = core.module(ShulkerPreviewModule.class);
        if (shulker != null && shulker.shouldPreview()) {
            TooltipComponent data = ShulkerTooltipHook.create(stack, shulker.tint());
            if (data != null) {
                cir.setReturnValue(Optional.of(data));
                return;
            }
        }
        FoodInfoModule food = core.module(FoodInfoModule.class);
        if (food != null && food.showTooltip()) {
            TooltipComponent data = FoodTooltip.create(stack);
            if (data != null) cir.setReturnValue(Optional.of(data));
        }
    }
}
