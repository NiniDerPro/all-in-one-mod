package dev.aio.fabric;

import dev.aio.core.AioCore;
import dev.aio.core.event.Events;
import dev.aio.core.util.Log;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.recipe.v1.sync.ClientRecipeSynchronizedEvent;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import dev.aio.core.modules.hud.HeldItemInfoModule;
import dev.aio.core.modules.player.FoodInfoModule;
import dev.aio.core.modules.hud.PotionStatusModule;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Einstiegspunkt des 26.3-Adapters: verbindet Fabric-Events mit dem Kern-Event-Bus. */
public final class AioFabricClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("aio");

    @Override
    public void onInitializeClient() {
        Log.sink = (level, message, error) -> {
            switch (level) {
                case "ERROR" -> LOGGER.error(message, error);
                case "WARN" -> LOGGER.warn(message);
                default -> LOGGER.info(message);
            }
        };

        Minecraft client = Minecraft.getInstance();
        FabricPlatform platform = new FabricPlatform(client);
        AioCore core = AioCore.init(platform, FabricLoader.getInstance().getConfigDir().resolve("aio"));
        FabricRenderer hudRenderer = new FabricRenderer(client);

        // Ganz oben über allen Vanilla-HUD-Elementen zeichnen (F1 blendet das HUD samt diesem Element aus).
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("aio", "hud"), (graphics, deltaTracker) -> {
            if (hideHudFor(client.gui.screen())) return;
            hudRenderer.begin(graphics);
            core.bus().post(new Events.RenderHud(hudRenderer, deltaTracker.getGameTimeDeltaPartialTick(false)));
        });

        // Vanilla-Teile ausblenden, die ein aktives AIO-Modul ersetzt
        HudElementRegistry.replaceElement(VanillaHudElements.MOB_EFFECTS, vanilla -> (graphics, delta) -> {
            PotionStatusModule potions = core.module(PotionStatusModule.class);
            if (potions == null || !potions.isActive()) vanilla.extractRenderState(graphics, delta);
        });
        HudElementRegistry.replaceElement(VanillaHudElements.HELD_ITEM_TOOLTIP, vanilla -> (graphics, delta) -> {
            HeldItemInfoModule info = core.module(HeldItemInfoModule.class);
            if (info == null || !info.replacesVanillaName()) vanilla.extractRenderState(graphics, delta);
        });

        // Essens-Info direkt an Hunger- und Lebensleiste (Positionen wie Vanilla: Mitte ± 91, 39 über unten)
        FabricRenderer foodRenderer = new FabricRenderer(client);
        HudElementRegistry.attachElementBefore(VanillaHudElements.FOOD_BAR, Identifier.fromNamespaceAndPath("aio", "food_under"), (g, d) -> {
            FoodInfoModule food = core.module(FoodInfoModule.class);
            if (food == null || !food.isActive()) return;
            foodRenderer.begin(g);
            food.renderUnderFood(foodRenderer, platform.foodState(), g.guiWidth() / 2f + 91, g.guiHeight() - 39);
        });
        HudElementRegistry.attachElementAfter(VanillaHudElements.FOOD_BAR, Identifier.fromNamespaceAndPath("aio", "food_over"), (g, d) -> {
            FoodInfoModule food = core.module(FoodInfoModule.class);
            if (food == null || !food.isActive()) return;
            foodRenderer.begin(g);
            food.renderOverFood(foodRenderer, platform.foodState(), g.guiWidth() / 2f + 91, g.guiHeight() - 39);
        });
        HudElementRegistry.attachElementAfter(VanillaHudElements.HEALTH_BAR, Identifier.fromNamespaceAndPath("aio", "health_over"), (g, d) -> {
            FoodInfoModule food = core.module(FoodInfoModule.class);
            if (food == null || !food.isActive()) return;
            foodRenderer.begin(g);
            food.renderOverHealth(foodRenderer, platform.foodState(), g.guiWidth() / 2f - 91, g.guiHeight() - 39);
        });

        ScreenHooks.register(core, client);
        BlockOutlineRenderer.register(core, client);
        ShulkerTooltipHook.register(core);
        ClientRecipeSynchronizedEvent.EVENT.register((c, recipes) -> RecipeSource.onSynced(recipes));

        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, timestamp) -> {
            boolean self = sender != null && client.player != null && sender.equals(client.player.getGameProfile());
            core.bus().post(new Events.ChatReceived(message.getString(), false, self));
        });
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) core.bus().post(new Events.ChatReceived(message.getString(), true));
        });

        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            // Fenster ohne Fokus (Alt+Tab): Loslassen kommt nicht mehr an, darum alles als losgelassen werten
            if (!c.isWindowActive()) InputState.clear();
            platform.applyPendingScreen();
            platform.saveOptionsIfDirty();
            core.bus().post(Events.Tick.INSTANCE);
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, c) -> {
            ServerData info = c.getCurrentServer();
            String address = c.hasSingleplayerServer() || info == null ? null : info.ip;
            core.bus().post(new Events.ServerJoin(address));
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, c) -> c.execute(() -> {
            RecipeSource.clear();
            core.bus().post(Events.ServerLeave.INSTANCE);
        }));

        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> core.shutdown());
    }

    /**
     * In Menüs und Inventaren (Werkbank, Truhe, Pause, ...) ist das AIO-HUD ausgeblendet.
     * Im Chat bleibt es sichtbar, die AIO-eigenen Screens zeichnen selbst, was sie brauchen.
     * Das Item-Panel rechts hängt am Inventar-Screen und ist davon nicht betroffen.
     */
    private static boolean hideHudFor(Screen screen) {
        return screen != null && !(screen instanceof ChatScreen) && !(screen instanceof AioScreen);
    }
}
