package dev.aio.fabric;

import dev.aio.core.AioCore;
import dev.aio.core.i18n.Lang;
import dev.aio.core.modules.chat.ChatModule;
import dev.aio.core.modules.chat.ChatResizer;
import dev.aio.core.modules.client.KeyBindsModule;
import dev.aio.core.modules.inventory.InventoryTweaksModule;
import dev.aio.core.modules.inventory.ItemListModule;
import dev.aio.core.modules.inventory.ItemPanel;
import dev.aio.core.platform.ItemRef;
import dev.aio.core.platform.Keys;
import dev.aio.fabric.mixin.AbstractContainerScreenAccessor;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Hooks the item list and mouse tweaks into inventory screens and opens "Copy chat"
 * on right click in chat. All via the Fabric screen API, without own mixins.
 */
final class ScreenHooks {
    private ScreenHooks() {}

    static void register(AioCore core, Minecraft client) {
        FabricRenderer renderer = new FabricRenderer(client);
        ScreenEvents.AFTER_INIT.register((mc, screen, w, h) -> {
            if (screen instanceof ChatScreen) hookChat(core, renderer, screen);
            // "AIO" button at the top left of the pause and title menu opens the AIO menu
            if ((screen instanceof PauseScreen || screen instanceof TitleScreen) && core.clientSettings().menuButton.on()) {
                Button open = Button.builder(Component.literal("AIO"), b -> core.openClickGui(null))
                        .bounds(6, 6, 44, 20)
                        .tooltip(Tooltip.create(Component.literal(Lang.tr("aio.ui.menu.open_aio", "Open the AIO menu"))))
                        .build();
                Screens.getWidgets(screen).add(open);
            }
            // Einmaliger Statistik-Hinweis oben im Titelbildschirm
            if (screen instanceof TitleScreen && StatsNotice.shouldShow(core)) {
                ScreenEvents.afterExtract(screen).register((s, graphics, mx, my, delta) -> {
                    renderer.begin(graphics);
                    StatsNotice.render(core, renderer, s.width);
                });
                ScreenMouseEvents.allowMouseClick(screen).register((s, event) ->
                        !StatsNotice.mouseClicked(core, event.x(), event.y(), s.width));
            }
            // Handelsfenster: Angebote des angeschauten Dorfbewohners merken (Dorfbewohner-Info)
            if (screen instanceof MerchantScreen merchant) {
                Entity target = client.hitResult instanceof EntityHitResult ehr ? ehr.getEntity() : null;
                if (target instanceof AbstractVillager) {
                    ScreenEvents.afterTick(screen).register(s -> VillagerOffers.remember(target, merchant.getMenu().getOffers()));
                }
            }
            // Key bindings with search instead of the vanilla list (back leads to the controls)
            if (screen instanceof KeyBindsScreen) {
                KeyBindsModule keys = core.module(KeyBindsModule.class);
                if (keys != null && keys.replacesVanilla()) keys.open();
            }
            // Also in the creative inventory (only the item list there, the mouse tweaks stay vanilla)
            if (screen instanceof AbstractContainerScreen<?> acs) {
                hookContainer(core, client, renderer, acs, screen instanceof CreativeModeInventoryScreen);
            }
        });
    }

    private static void hookChat(AioCore core, FabricRenderer renderer, Screen screen) {
        ChatResizer resizer = new ChatResizer(core);
        ScreenEvents.afterExtract(screen).register((s, graphics, mx, my, delta) -> {
            renderer.begin(graphics);
            resizer.render(renderer, mx, my);
        });
        ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> {
            int button = FabricPlatform.button(event.button());
            if (resizer.mouseClicked(renderer, event.x(), event.y(), button)) return false;
            ChatModule chat = core.module(ChatModule.class);
            if (button == 1 && chat != null && chat.isActive()) {
                chat.openCopy();
                return false;
            }
            return true;
        });
        ScreenMouseEvents.allowMouseDrag(screen).register((s, event, dx, dy) -> !resizer.mouseDragged(event.x(), event.y()));
        ScreenMouseEvents.allowMouseRelease(screen).register((s, event) -> !resizer.mouseReleased());
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

    /**
     * Our search field is not a vanilla widget. Since 26.3 (SDL) typed characters only arrive
     * while Minecraft has text input switched on, and it does that for a focused
     * text field. So an invisible vanilla text field holds the focus while our field is active.
     */
    private static void syncTextInput(Screen screen, ItemPanel panel, EditBox[] proxy) {
        boolean want = panel != null && panel.isFocused();
        if (want && proxy[0] == null) {
            EditBox box = new EditBox(Minecraft.getInstance().font, -1000, -1000, 1, 1, Component.empty());
            box.setFocused(true);
            screen.setFocused(box);
            proxy[0] = box;
        } else if (!want && proxy[0] != null) {
            proxy[0].setFocused(false);
            if (screen.getFocused() == proxy[0]) screen.setFocused(null);
            proxy[0] = null;
        }
    }

    private static void hookContainer(AioCore core, Minecraft client, FabricRenderer renderer, AbstractContainerScreen<?> screen,
                                      boolean creative) {
        AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) screen;
        ItemListModule list = core.module(ItemListModule.class);
        InventoryTweaksModule tweaks = core.module(InventoryTweaksModule.class);
        double[] mouse = new double[2];
        EditBox[] textProxy = new EditBox[1];

        ScreenEvents.afterExtract(screen).register((s, graphics, mx, my, delta) -> {
            mouse[0] = mx;
            mouse[1] = my;
            ItemPanel panel = list.panel();
            syncTextInput(s, panel, textProxy);
            if (panel == null) return;
            renderer.begin(graphics);
            // To the right of the inventory Minecraft shows the effects: leave this space free (compact width)
            int effects = core.platform().effects().isEmpty() ? 0 : 34;
            panel.render(renderer, mx, my, acc.aio$leftPos() + acc.aio$imageWidth() + effects);
        });

        ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> {
            ItemPanel panel = list.panel();
            boolean used = panel != null && panel.mouseClicked(renderer, event.x(), event.y(), FabricPlatform.button(event.button()));
            syncTextInput(s, panel, textProxy);
            if (used) return false;
            if (creative) return true;
            Slot slot = slotAt(screen, event.x(), event.y());
            int index = slot == null ? -1 : slot.index;
            if (FabricPlatform.button(event.button()) == 2) {
                boolean creativeMode = client.player != null && client.player.isCreative();
                if (tweaks.onMiddleClick(core.platform().container(), index, creativeMode)) return false;
            }
            if (FabricPlatform.button(event.button()) == 0 && shift()) tweaks.onDragStart(index);
            return true;
        });

        ScreenMouseEvents.allowMouseDrag(screen).register((s, event, dx, dy) -> {
            if (!creative && FabricPlatform.button(event.button()) == 0 && shift()) {
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
            if (creative) return true;
            Slot slot = slotAt(screen, mx, my);
            // Over bundles the mouse wheel selects the item in vanilla: do not interfere
            if (slot == null || slot.getItem().has(DataComponents.BUNDLE_CONTENTS)) return true;
            return !tweaks.onScroll(core.platform().container(), slot.index, v, shift());
        });

        ScreenKeyboardEvents.allowKeyPress(screen).register((s, event) -> {
            ItemPanel panel = list.panel();
            if (panel == null) return true;
            Slot slot = slotAt(screen, mouse[0], mouse[1]);
            ItemRef hovered = slot == null ? ItemRef.EMPTY : FabricItemRef.of(slot.getItem());
            boolean used = panel.keyPressed(FabricPlatform.keyName(event), hovered);
            syncTextInput(s, panel, textProxy);
            return !used;
        });

        ScreenKeyboardEvents.allowCharType(screen).register((s, event) -> {
            ItemPanel panel = list.panel();
            return panel == null || !panel.charTyped(event.codepointAsString());
        });

        ScreenEvents.remove(screen).register(s -> {
            ItemPanel panel = list.panel();
            if (panel != null) panel.onClose();
            syncTextInput(s, null, textProxy);
            tweaks.onRelease();
        });
    }
}
