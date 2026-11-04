package dev.aio.fabric.mixin;

import dev.aio.core.AioCore;
import dev.aio.core.modules.inventory.ShulkerPreviewModule;
import dev.aio.fabric.ShulkerTooltipHook;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/** Shulker-Vorschau: liefert das Raster als Tooltip-Bild. */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method = "getTooltipImage()Ljava/util/Optional;", at = @At("HEAD"), cancellable = true)
    private void aio$shulkerPreview(CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
        AioCore core = AioCore.get();
        if (core == null || !core.module(ShulkerPreviewModule.class).shouldPreview()) return;
        TooltipComponent data = ShulkerTooltipHook.create((ItemStack) (Object) this, core.module(ShulkerPreviewModule.class).tint());
        if (data != null) cir.setReturnValue(Optional.of(data));
    }
}
