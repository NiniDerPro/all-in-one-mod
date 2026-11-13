package dev.aio.fabric.mixin;

import dev.aio.core.AioCore;
import dev.aio.core.modules.inventory.ShulkerPreviewModule;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** The vanilla text list ("Totem of Undying x1", ...) is dropped while the shulker preview is active. */
@Mixin(ShulkerBoxBlock.class)
public abstract class ItemContainerContentsMixin {
    @Inject(method = "appendHoverText", at = @At("HEAD"), cancellable = true, require = 0)
    private void aio$hideContentLines(ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag, CallbackInfo ci) {
        AioCore core = AioCore.get();
        if (core == null) return;
        ShulkerPreviewModule m = core.module(ShulkerPreviewModule.class);
        if (m != null && m.isActive()) ci.cancel();
    }
}
