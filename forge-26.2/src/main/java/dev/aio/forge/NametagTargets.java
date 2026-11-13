package dev.aio.forge;

import dev.aio.core.AioCore;
import dev.aio.core.modules.render.NametagsModule;
import dev.aio.core.platform.ItemRef;
import dev.aio.core.platform.NametagTarget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Collects the objects for the nametags. Line of sight is checked at most once per tick. */
public final class NametagTargets {
    private NametagTargets() {}

    private static final EquipmentSlot[] GEAR = {EquipmentSlot.MAINHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST,
            EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFFHAND};
    private static final Map<Integer, Boolean> SIGHT = new HashMap<>();
    private static long sightTick = Long.MIN_VALUE;

    static List<NametagTarget> collect(Minecraft mc, double maxDistance, float partial, boolean players, boolean mobs,
                                       boolean items, boolean tnt, boolean throughWalls) {
        if (mc.level == null || mc.player == null) return List.of();
        long tick = mc.level.getGameTime();
        if (tick != sightTick) {
            SIGHT.clear();
            sightTick = tick;
        }
        Vec3 eye = mc.player.getEyePosition(partial);
        double max2 = maxDistance * maxDistance;
        List<NametagTarget> out = new ArrayList<>();
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e == mc.player || e == mc.getCameraEntity() || e instanceof ArmorStand) continue;
            NametagTarget.Kind kind = kindOf(e);
            if (kind == null) continue;
            if (!(kind == NametagTarget.Kind.PLAYER ? players : kind == NametagTarget.Kind.MOB ? mobs
                    : kind == NametagTarget.Kind.ITEM ? items : tnt)) continue;
            if (e.isInvisibleTo(mc.player)) continue;
            Vec3 feet = e.getPosition(partial);
            double d2 = feet.distanceToSqr(eye);
            if (d2 > max2) continue;
            if (!throughWalls && !SIGHT.computeIfAbsent(e.getId(), id -> mc.player.hasLineOfSight(e))) continue;
            out.add(build(mc, e, kind, feet, Math.sqrt(d2)));
        }
        return out;
    }

    static NametagTarget.Kind kindOf(Entity e) {
        if (e instanceof Player) return NametagTarget.Kind.PLAYER;
        if (e instanceof ItemEntity) return NametagTarget.Kind.ITEM;
        if (e instanceof PrimedTnt) return NametagTarget.Kind.TNT;
        if (e instanceof LivingEntity && !(e instanceof ArmorStand)) return NametagTarget.Kind.MOB;
        return null;
    }

    private static NametagTarget build(Minecraft mc, Entity e, NametagTarget.Kind kind, Vec3 feet, double dist) {
        double y = feet.y + e.getBbHeight() + (kind == NametagTarget.Kind.ITEM ? 0.45 : 0.5);
        String name = e.getDisplayName().getString();
        int nameColor = 0xFFFFFFFF;
        float hp = 0, max = 0, abs = 0;
        int ping = -1, count = 0, fuse = 0;
        String mode = "";
        List<ItemRef> gear = List.of();
        if (e instanceof LivingEntity le) {
            hp = le.getHealth();
            max = le.getMaxHealth();
            abs = le.getAbsorptionAmount();
            // Equipment on players and mobs (armored zombies are the dangerous ones)
            List<ItemRef> list = new ArrayList<>(GEAR.length);
            for (EquipmentSlot slot : GEAR) list.add(ForgeItemRef.of(le.getItemBySlot(slot)));
            gear = list;
        }
        if (e instanceof Player p) {
            name = p.getGameProfile().name();
            Integer team = p.getTeamColor() == 0xFFFFFF ? null : p.getTeamColor();
            if (team != null) nameColor = 0xFF000000 | team;
            PlayerInfo info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(p.getUUID());
            if (info != null) {
                ping = info.getLatency();
                mode = shortMode(info.getGameMode());
            }
        } else if (e instanceof ItemEntity item) {
            name = item.getItem().getHoverName().getString();
            count = item.getItem().getCount();
            nameColor = ForgeItemRef.of(item.getItem()).nameColor();
        } else if (e instanceof PrimedTnt t) {
            fuse = t.getFuse();
            y = feet.y + 1.25;
        }
        return new NametagTarget(kind, feet.x, y, feet.z, dist, name, nameColor, hp, max, abs, ping, mode, gear, count, fuse);
    }

    private static String shortMode(GameType type) {
        if (type == null) return "";
        return switch (type) {
            case SURVIVAL -> "S";
            case CREATIVE -> "C";
            case ADVENTURE -> "A";
            case SPECTATOR -> "Sp";
        };
    }

    /** For the mixin: should the vanilla nametag of this entity be hidden? */
    public static boolean hidesVanilla(Entity e) {
        AioCore core = AioCore.get();
        NametagsModule m = core == null ? null : core.module(NametagsModule.class);
        if (m == null || !m.isActive() || e == Minecraft.getInstance().player) return false;
        NametagTarget.Kind kind = kindOf(e);
        return kind != null && m.replaces(kind);
    }
}
