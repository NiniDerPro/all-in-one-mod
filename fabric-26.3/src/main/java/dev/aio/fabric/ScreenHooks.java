package dev.aio.fabric;

import dev.aio.core.AioCore;
import dev.aio.core.modules.chat.ChatModule;
import dev.aio.core.modules.inventory.InventoryTweaksModule;
import dev.aio.core.modules.inventory.ItemListModule;
import dev.aio.core.modules.inventory.ItemPanel;
import dev.aio.core.platform.ItemRef;
import dev.aio.core.platform.Keys;
import dev.aio.fabric.mixin.AbstractContainerScreenAccessor;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.Slot;

/**
 * Hängt Item-Liste und Maus-Tweaks in Inventar-Screens ein und öffnet "Chat kopieren"
 * per Rechtsklick im Chat. Alles über die Fabric Screen API, ohne eigene Mixins.
 */
final class ScreenHooks {
    private ScreenHooks() {}

    static void register(AioCore core, Minecraft client) {
        FabricRenderer renderer = new FabricRenderer(client);
        ScreenEvents.AFTER_INIT.register((mc, screen, w, h) -> {
            if (screen instanceof ChatScreen) hookChat(core, screen);
            if (screen instanceof AbstractContainerScreen<?> acs && !(screen instanceof CreativeModeInventoryScreen)) {
                hookContainer(core, client, renderer, acs);
            }
        });
    }

    private static void hookChat(AioCore core, Screen screen) {
        ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> {
            ChatModule chat = core.module(ChatModule.class);
            if (FabricPlatform.button(event.button()) == 1 && chat != null && chat.isActive()) {
                chat.openCopy();
                return false;
            }
            return true;
        });
    }

    private static boolean shift() {
        return InputState.isDown(Keys.LEFT_SHIFT) || InputState.isDown(Keys.RIGHT_SHIFT);
    }

    private static Slot slotAt(AbstractContainerScreen<?> screen, double mx, double my) {
        AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) screen;
        for (Slot s : screen.getMenu().slots) {
            if (!s.isActive()) continue;
            double x = acc.aio$leftPos() + s.x, y = acc.aio$topPos() + s.y;
            if (mx >= x - 1 && mx < x + 17 && my >= y - 1 && my < y + 17) return s;
        }
        return null;
    }

    private static void hookContainer(AioCore core, Minecraft client, FabricRenderer renderer, AbstractContainerScreen<?> screen) {
        AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) screen;
        ItemListModule list = core.module(ItemListModule.class);
        InventoryTweaksModule tweaks = core.module(InventoryTweaksModule.class);
        double[] mouse = new double[2];

        ScreenEvents.afterExtract(screen).register((s, graphics, mx, my, delta) -> {
            mouse[0] = mx;
            mouse[1] = my;
            ItemPanel panel = list.panel();
            if (panel == null) return;
            renderer.begin(graphics);
            panel.render(renderer, mx, my, acc.aio$leftPos() + acc.aio$imageWidth());
        });

        ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> {
            ItemPanel panel = list.panel();
            if (panel != null && panel.mouseClicked(renderer, event.x(), event.y(), FabricPlatform.button(event.button()))) return false;
            Slot slot = slotAt(screen, event.x(), event.y());
            int index = slot == null ? -1 : slot.index;
            if (FabricPlatform.button(event.button()) == 2) {
                boolean creative = client.player != null && client.player.isCreative();
                if (tweaks.onMiddleClick(core.platform().container(), index, creative)) return false;
            }
            if (FabricPlatform.button(event.button()) == 0 && shift()) tweaks.onDragStart(index);
            return true;
        });

        ScreenMouseEvents.allowMouseDrag(screen).register((s, event, dx, dy) -> {
            if (FabricPlatform.button(event.button()) == 0 && shift()) {
                Slot slot = slotAt(screen, event.x(), event.y());
                if (slot != null) tweaks.onDrag(core.platform().container(), slot.index, true);
            }
            return true;
        });

        ScreenMouseEvents.afterMouseRelease(screen).register((s, event, consumed) -> {
            tweaks.onRelease();
            return consumed;
        });

        ScreenMouseEvents.allowMouseScroll(screen).register((s, mx, my, h, v) -> {
            ItemPanel panel = list.panel();
            if (panel != null && panel.mouseScrolled(mx, my, v)) return false;
            Slot slot = slotAt(screen, mx, my);
            // Über Bündeln wählt das Mausrad in Vanilla das Item aus: nicht dazwischenfunken
            if (slot == null || slot.getItem().has(DataComponents.BUNDLE_CONTENTS)) return true;
            return !tweaks.onScroll(core.platform().container(), slot.index, v, shift());
        });

        ScreenKeyboardEvents.allowKeyPress(screen).register((s, event) -> {
            ItemPanel panel = list.panel();
            if (panel == null) return true;
            Slot slot = slotAt(screen, mouse[0], mouse[1]);
            ItemRef hovered = slot == null ? ItemRef.EMPTY : FabricItemRef.of(slot.getItem());
            return !panel.keyPressed(FabricPlatform.keyName(event), hovered);
        });

        ScreenKeyboardEvents.allowCharType(screen).register((s, event) -> {
            ItemPanel panel = list.panel();
            return panel == null || !panel.charTyped(event.codepointAsString());
        });

        ScreenEvents.remove(screen).register(s -> {
            ItemPanel panel = list.panel();
            if (panel != null) panel.onClose();
            tweaks.onRelease();
        });
    }
}
