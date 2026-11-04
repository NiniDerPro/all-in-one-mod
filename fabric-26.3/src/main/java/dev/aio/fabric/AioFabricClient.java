package dev.aio.fabric;

import dev.aio.core.AioCore;
import dev.aio.core.event.Events;
import dev.aio.core.util.Log;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
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
            hudRenderer.begin(graphics);
            core.bus().post(new Events.RenderHud(hudRenderer, deltaTracker.getGameTimeDeltaPartialTick(false)));
        });

        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            platform.applyPendingScreen();
            core.bus().post(Events.Tick.INSTANCE);
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, c) -> {
            ServerData info = c.getCurrentServer();
            String address = c.hasSingleplayerServer() || info == null ? null : info.ip;
            core.bus().post(new Events.ServerJoin(address));
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, c) -> c.execute(() -> core.bus().post(Events.ServerLeave.INSTANCE)));

        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> core.shutdown());
    }
}
