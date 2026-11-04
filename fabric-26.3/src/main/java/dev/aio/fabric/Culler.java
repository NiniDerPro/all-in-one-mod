package dev.aio.fabric;

import dev.aio.core.AioCore;
import dev.aio.core.modules.performance.EntityCullingModule;
import dev.aio.core.modules.performance.RenderDistanceModule;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sichtbarkeit für Entity-Culling. Strahlen vom Auge zu Punkten der Hitbox; ist jeder Strahl durch
 * einen undurchsichtigen Vollblock versperrt, gilt das Objekt als verdeckt.
 *
 * <p>Die Rechnung läuft in einem eigenen Thread: Das Spiel fragt nur das letzte Ergebnis ab und
 * meldet veraltete Objekte zur Neuberechnung an. Neue Objekte gelten bis zur ersten Prüfung als
 * sichtbar (kein Aufblitzen-Lassen von Lücken, höchstens kurz zu viel gezeichnet).
 */
public final class Culler {
    private Culler() {}

    /** Ergebnis: (Tick << 1) | sichtbar. */
    private static final Map<Long, Long> ENTITIES = new ConcurrentHashMap<>();
    private static final Map<Long, Long> BLOCKS = new ConcurrentHashMap<>();
    /** Anfragen an den Prüf-Thread: Schlüssel → Hitbox. */
    private static final Map<Long, AABB> PENDING_ENTITIES = new ConcurrentHashMap<>();
    private static final Map<Long, AABB> PENDING_BLOCKS = new ConcurrentHashMap<>();
    private static final Map<EntityType<?>, String> TYPE_IDS = new ConcurrentHashMap<>();
    private static final Map<BlockEntityType<?>, String> BE_TYPE_IDS = new ConcurrentHashMap<>();

    private static volatile long tick;
    /** Stand für den Prüf-Thread (vom Spiel-Thread jeden Tick gesetzt). */
    private static volatile Vec3 eye;
    private static volatile Level level;
    private static volatile boolean firstPerson = true;
    private static volatile boolean leavesSolid;
    private static volatile double traceDistance = 64;

    /** Gemessene Zeit im Prüf-Thread und Zähler (für die Diagnose). */
    public static volatile long nanos;
    public static volatile int culledEntities, culledBlockEntities;

    private static final int REFRESH_TICKS = 2;
    private static Thread worker;

    /** Jeden Client-Tick vom Spiel-Thread: Stand für den Prüf-Thread aktualisieren, Altes aufräumen. */
    public static void tick() {
        tick++;
        Minecraft mc = Minecraft.getInstance();
        EntityCullingModule m = module();
        if (mc.player == null || mc.level == null || m == null || !m.isActive()) {
            level = null;
            return;
        }
        eye = mc.player.getEyePosition();
        level = mc.level;
        firstPerson = mc.options.getCameraType() == CameraType.FIRST_PERSON;
        leavesSolid = m.leavesSolid();
        traceDistance = m.traceDistance();
        if (tick % 100 == 0) {
            long old = tick - 100;
            ENTITIES.values().removeIf(v -> (v >>> 1) < old);
            BLOCKS.values().removeIf(v -> (v >>> 1) < old);
        }
        ensureWorker();
    }

    private static synchronized void ensureWorker() {
        if (worker != null && worker.isAlive()) return;
        worker = new Thread(Culler::run, "AIO Entity-Culling");
        worker.setDaemon(true);
        worker.setPriority(Thread.NORM_PRIORITY - 1);
        worker.start();
    }

    /** Prüf-Thread: Anfragen abarbeiten, sonst kurz schlafen. */
    private static void run() {
        while (true) {
            try {
                Level lvl = level;
                Vec3 from = eye;
                boolean work = lvl != null && from != null && (!PENDING_ENTITIES.isEmpty() || !PENDING_BLOCKS.isEmpty());
                if (!work) {
                    Thread.sleep(4);
                    continue;
                }
                long t0 = System.nanoTime();
                int ce = 0, cb = 0;
                ce += process(PENDING_ENTITIES, ENTITIES, lvl, from);
                cb += process(PENDING_BLOCKS, BLOCKS, lvl, from);
                nanos += System.nanoTime() - t0;
                culledEntities = ce;
                culledBlockEntities = cb;
            } catch (InterruptedException e) {
                return;
            } catch (Throwable t) {
                // Welt wurde gerade umgebaut o. Ä.: beim nächsten Durchlauf erneut
            }
        }
    }

    private static int process(Map<Long, AABB> pending, Map<Long, Long> results, Level lvl, Vec3 from) {
        int culled = 0;
        for (var it = pending.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            it.remove();
            boolean vis;
            try {
                vis = compute(lvl, from, e.getValue());
            } catch (Throwable t) {
                vis = true; // im Zweifel zeigen
            }
            if (!vis) culled++;
            results.put(e.getKey(), (tick << 1) | (vis ? 1 : 0));
        }
        return culled;
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
        if (m == null || !m.cullEntities() || exempt(e, m)) return true;
        return visible(ENTITIES, PENDING_ENTITIES, e.getId(), e.getBoundingBox());
    }

    private static boolean exempt(Entity e, EntityCullingModule m) {
        if (e instanceof Player || e.isCurrentlyGlowing() || e.isVehicle() || e.isPassenger()) return true;
        if (m.keepNamed() && e.hasCustomName()) return true;
        String id = TYPE_IDS.computeIfAbsent(e.getType(), t -> String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(t)));
        return m.isEntityException(id);
    }

    /** Soll die Entity nur vereinfacht ticken? (zuletzt als verdeckt erkannt) */
    public static boolean skipTick(Entity e) {
        EntityCullingModule m = module();
        if (m == null || !m.cullTicks()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || e == mc.player || e == mc.getCameraEntity() || exempt(e, m)) return false;
        Long v = ENTITIES.get((long) e.getId());
        return v != null && (v & 1) == 0 && tick - (v >>> 1) <= REFRESH_TICKS + 2;
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
        String id = BE_TYPE_IDS.computeIfAbsent(be.getType(), t -> String.valueOf(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(t)));
        if (m.isBlockEntityException(id)) return true;
        return visible(BLOCKS, PENDING_BLOCKS, pos.asLong(), new AABB(pos));
    }

    /** Letztes Ergebnis; veraltet oder unbekannt → beim Prüf-Thread anfragen. */
    private static boolean visible(Map<Long, Long> results, Map<Long, AABB> pending, long key, AABB box) {
        Long v = results.get(key);
        if (v == null || tick - (v >>> 1) >= REFRESH_TICKS + (key & 1)) pending.put(key, box);
        return v == null || (v & 1) == 1;
    }

    private static boolean compute(Level lvl, Vec3 from, AABB box) {
        Vec3 center = box.getCenter();
        double dist = from.distanceTo(center);
        if (dist < 4 || dist > traceDistance) return true; // nah: immer zeigen; weit: nicht prüfen
        if (!firstPerson && dist < 10) return true;
        if (box.contains(from)) return true;
        AABB b = box.inflate(0.05);
        double[][] points = {
                {center.x, center.y, center.z},
                {center.x, b.maxY, center.z},
                {b.minX, b.minY, b.minZ}, {b.maxX, b.maxY, b.maxZ},
                {b.minX, b.maxY, b.maxZ}, {b.maxX, b.minY, b.minZ},
                {b.minX, b.maxY, b.minZ}, {b.maxX, b.minY, b.maxZ}};
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (double[] p : points) if (clear(lvl, cursor, from.x, from.y, from.z, p[0], p[1], p[2])) return true;
        return false;
    }

    private static boolean occludes(Level lvl, BlockPos pos) {
        BlockState state = lvl.getBlockState(pos);
        if (state.isSolidRender()) return true;
        return leavesSolid && state.getBlock() instanceof LeavesBlock;
    }

    /** Strahl durch das Blockraster (Schritt für Schritt von Block zu Block); frei, wenn kein Vollblock im Weg ist. */
    private static boolean clear(Level lvl, BlockPos.MutableBlockPos cursor, double x0, double y0, double z0, double x1, double y1, double z1) {
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
            cursor.set(x, y, z);
            if (occludes(lvl, cursor)) return false;
        }
        return true;
    }

    private static int floor(double v) { return (int) Math.floor(v); }
}
