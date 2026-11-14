package dev.aio.neoforge;

import dev.aio.core.platform.HealthTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Entities nearby for the Health Indicator. Line of sight is only checked once per tick. */
final class HealthTargets {
    private HealthTargets() {}

    /** Built once per tick: entity, finished entry and height above the feet; per frame only the position moves. */
    private record Cached(LivingEntity entity, HealthTarget target, double yOffset) {}
    private static final List<Cached> CACHE = new ArrayList<>();
    private static long cacheTick = Long.MIN_VALUE;
    private static String cacheKey = "";

    static List<HealthTarget> collect(Minecraft mc, double maxDistance, float partial, boolean players, boolean hostile,
                                      boolean passive, boolean throughWalls) {
        if (mc.level == null || mc.player == null) return List.of();
        long tick = mc.level.getGameTime();
        String key = maxDistance + "|" + players + hostile + passive + throughWalls;
        if (tick != cacheTick || !key.equals(cacheKey)) {
            cacheTick = tick;
            cacheKey = key;
            rebuild(mc, maxDistance + 4, players, hostile, passive, throughWalls);
        }
        Vec3 eye = mc.player.getEyePosition(partial);
        double max2 = maxDistance * maxDistance;
        List<HealthTarget> out = new ArrayList<>(CACHE.size());
        for (Cached c : CACHE) {
            if (c.entity().isRemoved()) continue;
            Vec3 feet = c.entity().getPosition(partial);
            double d2 = feet.distanceToSqr(eye);
            if (d2 > max2) continue;
            HealthTarget t = c.target();
            out.add(new HealthTarget(feet.x, feet.y + c.yOffset(), feet.z, Math.sqrt(d2), t.health(), t.maxHealth(), t.absorption(), t.player()));
        }
        return out;
    }

    private static void rebuild(Minecraft mc, double maxDistance, boolean players, boolean hostile, boolean passive, boolean throughWalls) {
        CACHE.clear();
        Vec3 eye = mc.player.getEyePosition();
        double max2 = maxDistance * maxDistance;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof LivingEntity le) || e == mc.player || e == mc.getCameraEntity() || e instanceof ArmorStand) continue;
            if (!le.isAlive() || le.isInvisibleTo(mc.player)) continue;
            boolean isPlayer = e instanceof Player;
            if (isPlayer ? !players : e instanceof Enemy ? !hostile : !passive) continue;
            Vec3 feet = e.position();
            double d2 = feet.distanceToSqr(eye);
            if (d2 > max2) continue;
            if (!throughWalls && !mc.player.hasLineOfSight(e)) continue;
            // above the nametag (players, named mobs), otherwise just above the head
            boolean nameTag = isPlayer || e.shouldShowName();
            double yOff = e.getBbHeight() + (nameTag ? 0.8 : 0.35);
            CACHE.add(new Cached(le, new HealthTarget(feet.x, feet.y + yOff, feet.z, Math.sqrt(d2), le.getHealth(), le.getMaxHealth(),
                    le.getAbsorptionAmount(), isPlayer), yOff));
        }
    }
}
