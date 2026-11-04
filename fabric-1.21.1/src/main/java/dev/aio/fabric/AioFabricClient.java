package dev.aio.fabric;

import dev.aio.core.AioCore;
import dev.aio.core.event.Events;
import dev.aio.core.util.Log;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Einstiegspunkt des 1.21.1-Adapters: verbindet Fabric-Events mit dem Kern-Event-Bus. */
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

        MinecraftClient client = MinecraftClient.getInstance();
        AioCore core = AioCore.init(new FabricPlatform(client), FabricLoader.getInstance().getConfigDir().resolve("aio"));
        FabricRenderer hudRenderer = new FabricRenderer(client);

        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            if (client.options.hudHidden) return;
            hudRenderer.begin(context);
            core.bus().post(new Events.RenderHud(hudRenderer, tickCounter.getTickDelta(false)));
        });

        ClientTickEvents.END_CLIENT_TICK.register(c -> core.bus().post(Events.Tick.INSTANCE));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, c) -> c.execute(() -> {
            ServerInfo info = c.getCurrentServerEntry();
            String address = c.isInSingleplayer() || info == null ? null : info.address;
            core.bus().post(new Events.ServerJoin(address));
        }));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, c) -> c.execute(() -> core.bus().post(Events.ServerLeave.INSTANCE)));

        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> core.shutdown());
    }
}
