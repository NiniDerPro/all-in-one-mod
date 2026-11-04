package dev.aio.fabric;

import dev.aio.core.AioCore;
import dev.aio.core.modules.performance.EntityCullingModule;
import dev.aio.core.modules.performance.RenderDistanceModule;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Sichtbarkeit für Entity-Culling: Strahlen vom Auge zu einigen Punkten der Hitbox; ist jeder Strahl
 * durch einen undurchsichtigen Vollblock versperrt, gilt das Objekt als verdeckt. Ergebnisse werden
 * pro Objekt einige Ticks lang gemerkt und versetzt neu berechnet, damit es pro Frame billig bleibt.
 */
public final class Culler {
    private Culler() {}

    /** Entity-ID bzw. Blockposition → (Tick << 1) | sichtbar. */
    private static final Long2LongOpenHashMap ENTITIES = new Long2LongOpenHashMap();
    private static final Long2LongOpenHashMap BLOCKS = new Long2LongOpenHashMap();
    private static long tick;
    /** Höchstens so viele Sichtprüfungen pro Tick, damit es auch mit vielen Mobs/Truhen billig bleibt. */
    private static final int BUDGET_PER_TICK = 48;
    private static int budget = BUDGET_PER_TICK;
    /** Gemessene Zeit für Sichtprüfungen (Nanosekunden, für die Diagnose). */
    public static long nanos;
    private static final BlockPos.MutableBlockPos CURSOR = new BlockPos.MutableBlockPos();

    /** Jeden Client-Tick: Zähler weiter, alte Einträge ab und zu wegwerfen. */
    public static void tick() {
        tick++;
        budget = BUDGET_PER_TICK;
        if (tick % 200 == 0) {
            ENTITIES.clear();
            BLOCKS.clear();
        }
    }

    private static EntityCullingModule module() {
        AioCore core = AioCore.get();
        return core == null ? null : core.module(EntityCullingModule.class);
    }

    private static RenderDistanceModule distanceModule() {
        AioCore core = AioCore.get();
        if (core == null) return null;
        RenderDistanceModule m = core.module(RenderDistanceModule.class);
        return m != null && m.isActive() ? m : null;
    }

    /** Soll die Entity gezeichnet werden? (Entfernung und Verdeckung) */
    public static boolean shouldRenderEntity(Entity e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || e == mc.player || e == mc.getCameraEntity()) return true;
        RenderDistanceModule dist = distanceModule();
        if (dist != null && !(e instanceof Player && dist.keepPlayers())) {
            double d = dist.entityDistance();
            if (e.distanceToSqr(mc.player) > d * d) return false;
        }
        EntityCullingModule m = module();
        if (m == null || !m.cullEntities()) return true;
        if (e instanceof Player || e.isCurrentlyGlowing() || e.isVehicle() || e.isPassenger()) return true;
        return visible(ENTITIES, e.getId(), e.getBoundingBox(), m);
    }

    /** Soll die Entity nur vereinfacht ticken? (verdeckt und nichts Besonderes) */
    public static boolean skipTick(Entity e) {
        EntityCullingModule m = module();
        if (m == null || !m.cullTicks()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || e == mc.player || e == mc.getCameraEntity() || e instanceof Player
                || e.isVehicle() || e.isPassenger()) return false;
        long v = ENTITIES.getOrDefault(e.getId(), -1L);
        return v >= 0 && (v & 1) == 0 && tick - (v >>> 1) <= m.refreshTicks();
    }

    /** Soll die Block-Entity gezeichnet werden? */
    public static boolean shouldRenderBlockEntity(BlockEntity be) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return true;
        BlockPos pos = be.getBlockPos();
        RenderDistanceModule dist = distanceModule();
        if (dist != null) {
            double d = be instanceof SignBlockEntity || be instanceof HangingSignBlockEntity ? dist.signDistance() : dist.blockEntityDistance();
            if (mc.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > d * d) return false;
        }
        EntityCullingModule m = module();
        if (m == null || !m.cullBlockEntities()) return true;
        return visible(BLOCKS, pos.asLong(), new AABB(pos), m);
    }

    private static boolean visible(Long2LongOpenHashMap cache, long key, AABB box, EntityCullingModule m) {
        long v = cache.getOrDefault(key, -1L);
        int refresh = m.refreshTicks();
        // versetzt neu prüfen, damit nicht alles im selben Tick gerechnet wird
        if (v >= 0 && tick - (v >>> 1) < refresh + (key & 1)) return (v & 1) == 1;
        if (budget <= 0) return v < 0 || (v & 1) == 1; // Budget aufgebraucht: alten Stand behalten (neu = sichtbar)
        budget--;
        long t0 = System.nanoTime();
        boolean vis = compute(box, m.traceDistance());
        nanos += System.nanoTime() - t0;
        cache.put(key, (tick << 1) | (vis ? 1 : 0));
        return vis;
    }

    private static boolean compute(AABB box, double maxDistance) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null || mc.player == null) return true;
        Vec3 eye = mc.player.getEyePosition();
        Vec3 center = box.getCenter();
        double dist = eye.distanceTo(center);
        if (dist < 4 || dist > maxDistance) return true; // nah: immer zeigen; weit: nicht prüfen
        if (mc.options.getCameraType() != net.minecraft.client.CameraType.FIRST_PERSON && dist < 10) return true;
        if (box.contains(eye)) return true;
        AABB b = box.inflate(0.05);
        double[][] points = {
                {center.x, center.y, center.z},
                {center.x, b.maxY, center.z},
                {b.minX, b.minY, b.minZ}, {b.maxX, b.maxY, b.maxZ},
                {b.minX, b.maxY, b.maxZ}, {b.maxX, b.minY, b.minZ},
                {b.minX, b.maxY, b.minZ}, {b.maxX, b.minY, b.maxZ}};
        for (double[] p : points) if (clear(level, eye.x, eye.y, eye.z, p[0], p[1], p[2])) return true;
        return false;
    }

    /** Strahl durch das Blockraster (Schritt für Schritt von Block zu Block); frei, wenn kein Vollblock im Weg ist. */
    private static boolean clear(Level level, double x0, double y0, double z0, double x1, double y1, double z1) {
        int x = floor(x0), y = floor(y0), z = floor(z0);
        int ex = floor(x1), ey = floor(y1), ez = floor(z1);
        double dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        int sx = dx > 0 ? 1 : -1, sy = dy > 0 ? 1 : -1, sz = dz > 0 ? 1 : -1;
        double tdx = dx == 0 ? Double.MAX_VALUE : Math.abs(1 / dx);
        double tdy = dy == 0 ? Double.MAX_VALUE : Math.abs(1 / dy);
        double tdz = dz == 0 ? Double.MAX_VALUE : Math.abs(1 / dz);
        double tmx = dx == 0 ? Double.MAX_VALUE : (dx > 0 ? (x + 1 - x0) : (x0 - x)) * tdx;
        double tmy = dy == 0 ? Double.MAX_VALUE : (dy > 0 ? (y + 1 - y0) : (y0 - y)) * tdy;
        double tmz = dz == 0 ? Double.MAX_VALUE : (dz > 0 ? (z + 1 - z0) : (z0 - z)) * tdz;
        for (int guard = 0; guard < 512; guard++) {
            if (tmx < tmy && tmx < tmz) { x += sx; tmx += tdx; }
            else if (tmy < tmz) { y += sy; tmy += tdy; }
            else { z += sz; tmz += tdz; }
            if (x == ex && y == ey && z == ez) return true; // Ziel-Block erreicht
            CURSOR.set(x, y, z);
            if (level.getBlockState(CURSOR).isSolidRender()) return false;
        }
        return true;
    }

    private static int floor(double v) { return (int) Math.floor(v); }
}
