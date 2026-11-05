package dev.aio.fabric;

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

/** Lebewesen in der Nähe für den Health Indicator. Sichtlinie wird nur einmal pro Tick geprüft. */
final class HealthTargets {
    private HealthTargets() {}

    private static final Map<Integer, Boolean> SIGHT = new HashMap<>();
    private static long sightTick = Long.MIN_VALUE;

    static List<HealthTarget> collect(Minecraft mc, double maxDistance, float partial, boolean players, boolean hostile,
                                      boolean passive, boolean throughWalls) {
        if (mc.level == null || mc.player == null) return List.of();
        long tick = mc.level.getGameTime();
        if (tick != sightTick) {
            SIGHT.clear();
            sightTick = tick;
        }
        Vec3 eye = mc.player.getEyePosition(partial);
        double max2 = maxDistance * maxDistance;
        List<HealthTarget> out = new ArrayList<>();
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof LivingEntity le) || e == mc.player || e == mc.getCameraEntity() || e instanceof ArmorStand) continue;
            if (!le.isAlive() || le.isInvisibleTo(mc.player)) continue;
            boolean isPlayer = e instanceof Player;
            if (isPlayer ? !players : e instanceof Enemy ? !hostile : !passive) continue;
            Vec3 feet = e.getPosition(partial);
            double d2 = feet.distanceToSqr(eye);
            if (d2 > max2) continue;
            if (!throughWalls && !SIGHT.computeIfAbsent(e.getId(), id -> mc.player.hasLineOfSight(e))) continue;
            // über dem Namensschild (Spieler, benannte Mobs), sonst knapp über dem Kopf
            boolean nameTag = isPlayer || e.shouldShowName();
            double y = feet.y + e.getBbHeight() + (nameTag ? 0.8 : 0.35);
            out.add(new HealthTarget(feet.x, y, feet.z, Math.sqrt(d2), le.getHealth(), le.getMaxHealth(), le.getAbsorptionAmount(), isPlayer));
        }
        return out;
    }
}
