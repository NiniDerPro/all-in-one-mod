package dev.aio.neoforge;

import dev.aio.core.AioCore;
import dev.aio.core.modules.inventory.ShulkerPreviewModule;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
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

/** Connects the shulker preview with Fabric's tooltip events. */
public final class ShulkerTooltipHook {
    private ShulkerTooltipHook() {}

    /** Tooltip drawers; registered while the mod is constructed. */
    static void registerFactories(net.neoforged.bus.api.IEventBus modBus) {
        modBus.addListener(RegisterClientTooltipComponentFactoriesEvent.class, event -> {
            event.register(ShulkerTooltip.Data.class, ShulkerTooltip.Client::new);
            event.register(FoodTooltip.Data.class, FoodTooltip.Client::new);
        });
    }

    static void register(AioCore core) {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ItemTooltipEvent.class, event -> {
            var stack = event.getItemStack();
            var lines = event.getToolTip();
            ShulkerPreviewModule m = core.module(ShulkerPreviewModule.class);
            if (m == null || !m.isActive() || !isShulker(stack)) return;
            int stacks = stackCount(stack.get(DataComponents.CONTAINER));
            if (stacks == 0) return;
            // Replaces the vanilla list: short summary, below it the note about the key
            lines.add(Component.literal("Contains " + stacks + (stacks == 1 ? " Stack" : " Stacks")).withStyle(ChatFormatting.GRAY));
            if (m.showHint()) {
                lines.add(Component.literal("[Shift]").withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(" Show full contents").withStyle(ChatFormatting.GRAY)));
            }
        });
    }

    /** Occupied slots of a chest. */
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

    // The tooltip is queried every frame: reuse the result for the same content
    private static ItemContainerContents lastContents;
    private static boolean lastTint;
    private static TooltipComponent lastData;

    /** Tooltip data or null if the item is not a (filled) shulker box. */
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
