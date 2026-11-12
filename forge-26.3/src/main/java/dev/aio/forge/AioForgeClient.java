package dev.aio.forge;

import dev.aio.core.AioCore;
import dev.aio.core.event.Events;
import dev.aio.core.i18n.Lang;
import dev.aio.core.modules.hud.HeldItemInfoModule;
import dev.aio.core.modules.hud.PotionStatusModule;
import dev.aio.core.modules.performance.FpsBoostModule;
import dev.aio.core.modules.player.FoodInfoModule;
import dev.aio.core.util.Log;

import dev.aio.forge.shim.Loader;
import dev.aio.forge.shim.ScreenEvents;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.SystemMessageReceivedEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;
import net.minecraftforge.event.GameShuttingDownEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.locale.Language;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/** Client start of the 26.3 Forge adapter: connects Forge events with the core event bus. */
public final class AioForgeClient {
    private AioForgeClient() {}

    public static final Logger LOGGER = LoggerFactory.getLogger("aio");

    public static void init() {
        Log.sink = (level, message, error) -> {
            switch (level) {
                case "ERROR" -> LOGGER.error(message, error);
                case "WARN" -> LOGGER.warn(message);
                default -> LOGGER.info(message);
            }
        };

        // Texts from assets/aio/lang/*.json; follows the language setting of Minecraft
        Lang.setLookup(key -> {
            if (LangSwitch.forced()) return LangSwitch.get(key);
            Language language = Language.getInstance();
            return language.has(key) ? language.getOrDefault(key) : null;
        });

        Minecraft client = Minecraft.getInstance();
        ForgePlatform platform = new ForgePlatform(client);
        AioCore core = AioCore.init(platform, Loader.configDir().resolve("aio"));
        Telemetry.init(core);
        dev.aio.core.update.UpdateCheck.start(core, Loader.modVersion("aio").orElse(""));
        ChatWaypoints.register(core);
        ForgeRenderer hudRenderer = new ForgeRenderer(client);

        AddGuiOverlayLayersEvent.BUS.addListener(event -> {
            ForgeLayeredDraw layers = event.getLayeredDraw();
            // Draw at the very top above all vanilla HUD elements (F1 hides the HUD including this element).
            layers.addWithCondition(Identifier.fromNamespaceAndPath("aio", "hud"), (graphics, deltaTracker) -> {
                if (hideHudFor(client.gui.screen())) return;
                long t0 = System.nanoTime();
                hudRenderer.begin(graphics);
                core.bus().post(new Events.RenderHud(hudRenderer, deltaTracker.getGameTimeDeltaPartialTick(false)));
                hudNanos += System.nanoTime() - t0;
                hudFrames++;
            }, () -> !client.gui.hud.isHidden());
            // Hide vanilla parts that an active AIO module replaces
            layers.addConditionTo(ForgeLayeredDraw.PRE_SLEEP_STACK, ForgeLayeredDraw.POTION_EFFECTS, () -> {
                PotionStatusModule potions = core.module(PotionStatusModule.class);
                return potions == null || !potions.isActive();
            });
            layers.addConditionTo(ForgeLayeredDraw.HOTBAR_AND_DECOS, ForgeLayeredDraw.SELECTED_ITEM_NAME, () -> {
                HeldItemInfoModule info = core.module(HeldItemInfoModule.class);
                return info == null || !info.replacesVanillaName();
            });
            // Food info directly at the hunger and health bars (positions like vanilla: centre +- 91, 39 above the bottom)
            layers.addBelow(ForgeLayeredDraw.HOTBAR_AND_DECOS, Identifier.fromNamespaceAndPath("aio", "food_under"), ForgeLayeredDraw.HEALTH_BAR,
                    (g, d) -> FoodHud.renderUnder(g, core.module(FoodInfoModule.class), g.guiWidth() / 2 + 91, g.guiHeight() - 39));
            layers.addAbove(ForgeLayeredDraw.HOTBAR_AND_DECOS, Identifier.fromNamespaceAndPath("aio", "food_over"), ForgeLayeredDraw.HEALTH_BAR,
                    (g, d) -> FoodHud.renderOver(g, core.module(FoodInfoModule.class), g.guiWidth() / 2 + 91, g.guiHeight() - 39));
            layers.addAbove(ForgeLayeredDraw.HOTBAR_AND_DECOS, Identifier.fromNamespaceAndPath("aio", "health_over"), ForgeLayeredDraw.HEALTH_BAR,
                    (g, d) -> FoodHud.renderHealth(g, core.module(FoodInfoModule.class), g.guiWidth() / 2 - 91, g.guiHeight() - 39));
        });

        ScreenEvents.install();
        ScreenHooks.register(core, client);
        BlockOutlineRenderer.register(core, client);
        LightOverlay.register(core, client);
        ShulkerTooltipHook.register(core);
        ClientChatReceivedEvent.Player.BUS.addListener(event -> {
            boolean self = event.getSender() != null && client.player != null && event.getSender().equals(client.player.getUUID());
            core.bus().post(new Events.ChatReceived(event.getMessage().getString(), false, self));
            return false;
        });
        SystemMessageReceivedEvent.BUS.addListener(event -> {
            if (!event.isOverlay()) core.bus().post(new Events.ChatReceived(event.getMessage().getString(), true));
            return false;
        });

        TickEvent.ClientTickEvent.Pre.BUS.addListener(event -> Freecam.preTick());
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> {
            Minecraft c = client;
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

        ClientPlayerNetworkEvent.LoggingIn.BUS.addListener(event -> {
            ServerData info = client.getCurrentServer();
            String address = client.hasSingleplayerServer() || info == null ? null : info.ip;
            core.bus().post(new Events.ServerJoin(address));
        });
        ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(event -> client.execute(() -> {
            RecipeSource.clear();
            VillagerOffers.clear();
            ShieldIndicator.clear();
            core.bus().post(Events.ServerLeave.INSTANCE);
        }));

        GameShuttingDownEvent.BUS.addListener(event -> core.shutdown());
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
