package dev.aio.fabric;

import dev.aio.core.AioCore;
import dev.aio.core.modules.inventory.ShulkerPreviewModule;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/** Verbindet die Shulker-Vorschau mit den Tooltip-Events von Fabric. */
public final class ShulkerTooltipHook {
    private ShulkerTooltipHook() {}

    static void register(AioCore core) {
        ClientTooltipComponentCallback.EVENT.register(data -> data instanceof ShulkerTooltip.Data d ? new ShulkerTooltip.Client(d) : null);
        ClientTooltipComponentCallback.EVENT.register(data -> data instanceof FoodTooltip.Data d ? new FoodTooltip.Client(d) : null);
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            ShulkerPreviewModule m = core.module(ShulkerPreviewModule.class);
            if (m == null || !m.isActive() || !isShulker(stack)) return;
            int stacks = stackCount(stack.get(DataComponents.CONTAINER));
            if (stacks == 0) return;
            // Ersetzt die Vanilla-Liste: kurze Zusammenfassung, darunter der Hinweis auf die Taste
            lines.add(Component.literal("Enthält " + stacks + (stacks == 1 ? " Stack" : " Stacks")).withStyle(ChatFormatting.GRAY));
            if (m.showHint()) {
                lines.add(Component.literal("[Shift]").withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(" Gesamten Inhalt anzeigen").withStyle(ChatFormatting.GRAY)));
            }
        });
    }

    /** Belegte Slots einer Kiste. */
    static int stackCount(ItemContainerContents contents) {
        if (contents == null) return 0;
        NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
        contents.copyInto(items);
        int n = 0;
        for (ItemStack s : items) if (!s.isEmpty()) n++;
        return n;
    }

    static boolean isShulker(ItemStack stack) {
        return stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock;
    }

    // Der Tooltip wird jeden Frame abgefragt: Ergebnis für denselben Inhalt wiederverwenden
    private static ItemContainerContents lastContents;
    private static boolean lastTint;
    private static TooltipComponent lastData;

    /** Tooltip-Daten oder null, wenn das Item keine (gefüllte) Shulkerkiste ist. */
    public static TooltipComponent create(ItemStack stack, boolean tint) {
        if (!isShulker(stack)) return null;
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents == null) return null;
        if (contents == lastContents && tint == lastTint) return lastData;
        lastContents = contents;
        lastTint = tint;
        lastData = build(stack, contents, tint);
        return lastData;
    }

    private static TooltipComponent build(ItemStack stack, ItemContainerContents contents, boolean tint) {
        NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
        contents.copyInto(items);
        boolean any = false;
        for (ItemStack s : items) if (!s.isEmpty()) { any = true; break; }
        if (!any) return null;
        int color = 0;
        if (tint && ((BlockItem) stack.getItem()).getBlock() instanceof ShulkerBoxBlock box) {
            DyeColor dye = box.getColor();
            color = dye == null ? 0x9A7D9A : dye.getTextureDiffuseColor() & 0xFFFFFF;
        }
        return new ShulkerTooltip.Data(items, color);
    }
}
