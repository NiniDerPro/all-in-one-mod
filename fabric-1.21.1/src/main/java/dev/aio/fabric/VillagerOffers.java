package dev.aio.fabric;

import dev.aio.core.platform.WorldLabel;

import net.minecraft.client.Minecraft;

import java.util.List;

/** Villager trade labels: not available on this Minecraft version yet. */
final class VillagerOffers {
    private VillagerOffers() {}

    static void clear() {}

    static List<WorldLabel> labels(Minecraft client, double maxDistance, float partialTick) { return List.of(); }
}
