package dev.aio.fabric;

import dev.aio.core.AioCore;
import dev.aio.core.event.Events;
import dev.aio.core.i18n.Lang;
import dev.aio.core.modules.hud.HeldItemInfoModule;
import dev.aio.core.modules.hud.PotionStatusModule;
import dev.aio.core.modules.performance.FpsBoostModule;
import dev.aio.core.modules.player.FoodInfoModule;
import dev.aio.core.util.Log;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.recipe.v1.sync.ClientRecipeSynchronizedEvent;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.locale.Language;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/** Entry point of the 26.3 adapter: connects Fabric events with the core event bus. */
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

        // Texts from assets/aio/lang/*.json; follows the language setting of Minecraft
        Lang.setLookup(key -> {
            Language language = Language.getInstance();
            return language.has(key) ? language.getOrDefault(key) : null;
        });

        Minecraft client = Minecraft.getInstance();
        FabricPlatform platform = new FabricPlatform(client);
        AioCore core = AioCore.init(platform, FabricLoader.getInstance().getConfigDir().resolve("aio"));
        Telemetry.init(core);
        ChatWaypoints.register(core);
        FabricRenderer hudRenderer = new FabricRenderer(client);

        // Draw at the very top above all vanilla HUD elements (F1 hides the HUD including this element).
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("aio", "hud"), (graphics, deltaTracker) -> {
            if (hideHudFor(client.gui.screen())) return;
            long t0 = System.nanoTime();
            hudRenderer.begin(graphics);
            core.bus().post(new Events.RenderHud(hudRenderer, deltaTracker.getGameTimeDeltaPartialTick(false)));
            hudNanos += System.nanoTime() - t0;
            hudFrames++;
        });

        // Hide vanilla parts that an active AIO module replaces
        HudElementRegistry.replaceElement(VanillaHudElements.MOB_EFFECTS, vanilla -> (graphics, delta) -> {
            PotionStatusModule potions = core.module(PotionStatusModule.class);
            if (potions == null || !potions.isActive()) vanilla.extractRenderState(graphics, delta);
        });
        HudElementRegistry.replaceElement(VanillaHudElements.HELD_ITEM_TOOLTIP, vanilla -> (graphics, delta) -> {
            HeldItemInfoModule info = core.module(HeldItemInfoModule.class);
            if (info == null || !info.replacesVanillaName()) vanilla.extractRenderState(graphics, delta);
        });

        // Food info directly at the hunger and health bars (positions like vanilla: centre ± 91, 39 above the bottom)
        FabricRenderer foodRenderer = new FabricRenderer(client);
        HudElementRegistry.attachElementBefore(VanillaHudElements.FOOD_BAR, Identifier.fromNamespaceAndPath("aio", "food_under"),
                (g, d) -> FoodHud.renderUnder(g, core.module(FoodInfoModule.class), g.guiWidth() / 2 + 91, g.guiHeight() - 39));
        HudElementRegistry.attachElementAfter(VanillaHudElements.FOOD_BAR, Identifier.fromNamespaceAndPath("aio", "food_over"),
                (g, d) -> FoodHud.renderOver(g, core.module(FoodInfoModule.class), g.guiWidth() / 2 + 91, g.guiHeight() - 39));
        HudElementRegistry.attachElementAfter(VanillaHudElements.HEALTH_BAR, Identifier.fromNamespaceAndPath("aio", "health_over"),
                (g, d) -> FoodHud.renderHealth(g, core.module(FoodInfoModule.class), g.guiWidth() / 2 - 91, g.guiHeight() - 39));

        ScreenHooks.register(core, client);
        BlockOutlineRenderer.register(core, client);
        LightOverlay.register(core, client);
        ShulkerTooltipHook.register(core);
        ClientRecipeSynchronizedEvent.EVENT.register((c, recipes) -> RecipeSource.onSynced(recipes));

        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, timestamp) -> {
            boolean self = sender != null && client.player != null && sender.equals(client.player.getGameProfile());
            core.bus().post(new Events.ChatReceived(message.getString(), false, self));
        });
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) core.bus().post(new Events.ChatReceived(message.getString(), true));
        });

        ClientTickEvents.START_CLIENT_TICK.register(c -> Freecam.preTick());
        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            // Window without focus (Alt+Tab): releases no longer arrive, so treat everything as released
            if (!c.isWindowActive()) InputState.clear();
            platform.applyPendingScreen();
            Culler.tick();
            FoodHud.tick();
            Freecam.tick();
            LightOverlay.tick(core, c);
            DynamicLights.tick(c);
            DevSelfTest.tick(core, c);
            Telemetry.tick(c.getFps());
            long t0 = System.nanoTime();
            platform.saveOptionsIfDirty();
            core.bus().post(Events.Tick.INSTANCE);
            tickNanos += System.nanoTime() - t0;
            if (++diagTicks >= 600) reportPerformance(core);
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, c) -> {
            ServerData info = c.getCurrentServer();
            String address = c.hasSingleplayerServer() || info == null ? null : info.ip;
            core.bus().post(new Events.ServerJoin(address));
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, c) -> c.execute(() -> {
            RecipeSource.clear();
            VillagerOffers.clear();
            ShieldIndicator.clear();
            core.bus().post(Events.ServerLeave.INSTANCE);
        }));

        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> core.shutdown());
    }

    /**
     * In menus and inventories (crafting table, chest, pause, ...) the AIO HUD is hidden.
     * In chat it stays visible, the AIO's own screens draw what they need themselves.
     * The item panel on the right is attached to the inventory screen and is not affected.
     */
    private static boolean hideHudFor(Screen screen) {
        return screen != null && !(screen instanceof ChatScreen) && !(screen instanceof AioScreen);
    }

    // ---- Leistungs-Diagnose (FPS-Boost → "Leistungs-Diagnose ins Log") ----
    private static long hudNanos, hudFrames, tickNanos;
    private static int diagTicks;

    /** Every 30 seconds: how much time AIO itself needed (to compare with the frame time from F3). */
    private static void reportPerformance(AioCore core) {
        FpsBoostModule boost = core.module(FpsBoostModule.class);
        if (boost != null && boost.diagnose()) {
            double hud = hudFrames == 0 ? 0 : hudNanos / 1e6 / hudFrames;
            LOGGER.info(String.format(Locale.ROOT,
                    "[aio] Performance: HUD %.3f ms/frame (%d frames), ticks %.3f ms/tick, visibility checks %.3f ms/tick (own thread), "
                            + "last hidden: %d entities, %d block entities, FPS %d",
                    hud, hudFrames, tickNanos / 1e6 / diagTicks, Culler.nanos / 1e6 / diagTicks,
                    Culler.culledEntities, Culler.culledBlockEntities, Minecraft.getInstance().getFps()));
        }
        hudNanos = hudFrames = tickNanos = 0;
        Culler.nanos = 0;
        diagTicks = 0;
    }
}
