package dev.aio.fabric.mixin;

import dev.aio.core.AioCore;
import dev.aio.core.modules.inventory.ShulkerPreviewModule;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/** The vanilla text list ("Totem of Undying x1", ...) is dropped while the shulker preview is active. */
@Mixin(ItemContainerContents.class)
public abstract class ItemContainerContentsMixin {
    @Inject(method = "addToTooltip", at = @At("HEAD"), cancellable = true, require = 0)
    private void aio$hideContentLines(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag,
                                      DataComponentGetter components, CallbackInfo ci) {
        AioCore core = AioCore.get();
        if (core == null) return;
        ShulkerPreviewModule m = core.module(ShulkerPreviewModule.class);
        if (m != null && m.isActive()) ci.cancel();
    }
}
