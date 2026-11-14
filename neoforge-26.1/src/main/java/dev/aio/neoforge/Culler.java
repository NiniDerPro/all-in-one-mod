package dev.aio.neoforge;

import dev.aio.core.AioCore;
import dev.aio.core.modules.performance.EntityCullingModule;
import dev.aio.core.modules.performance.RenderDistanceModule;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.core.SectionPos;
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
 * Visibility for entity culling. Rays from the eye to points of the hitbox; if every ray is blocked by
 * an opaque full block, the object counts as hidden.
 *
 * <p>The computation runs in its own thread: the game only queries the last result and
 * registers outdated objects for recomputation. New objects count as visible until the first check
 * (no flashing gaps, at worst briefly drawn too much).
 */
public final class Culler {
    private Culler() {}

    /** Ergebnis: (Tick << 1) | sichtbar. */
    private static final Map<Long, Long> ENTITIES = new ConcurrentHashMap<>();
    private static final Map<Long, Long> BLOCKS = new ConcurrentHashMap<>();
    /** Requests to the check thread: key → hitbox. */
    private static final Map<Long, AABB> PENDING_ENTITIES = new ConcurrentHashMap<>();
    private static final Map<Long, AABB> PENDING_BLOCKS = new ConcurrentHashMap<>();
    private static final Map<EntityType<?>, String> TYPE_IDS = new ConcurrentHashMap<>();
    private static final Map<BlockEntityType<?>, String> BE_TYPE_IDS = new ConcurrentHashMap<>();

    private static volatile long tick;
    /** State for the check thread (set by the game thread every tick). */
    private static volatile Vec3 eye;
    private static volatile Level level;
    private static volatile boolean firstPerson = true;
    private static volatile boolean leavesSolid;
    private static volatile double traceDistance = 64;

    /** Measured time in the check thread and counter (for the diagnostics). */
    public static volatile long nanos;
    public static volatile int culledEntities, culledBlockEntities;

    private static final int REFRESH_TICKS = 2;

    /**
     * Opaque blocks per chunk section as 4096 bits, built on the game thread. The check thread only
     * reads these copies and never touches the world itself. Missing sections count as see-through.
     */
    private record Section(long[] bits, long built) {}
    private static final Map<Long, Section> SECTIONS = new ConcurrentHashMap<>();
    private static final java.util.Set<Long> PENDING_SECTIONS = ConcurrentHashMap.newKeySet();
    private static final int SECTIONS_PER_TICK = 24;
    private static Level sectionsLevel;
    /** Consecutive "hidden" results per object: only hide after two in a row (no flicker at edges). */
    private static final Map<Long, Integer> HIDDEN_STREAK = new ConcurrentHashMap<>();
    /** Index of the target point that was visible last time, tried first next time. */
    private static final Map<Long, Integer> LAST_POINT = new ConcurrentHashMap<>();
    private static Thread worker;

    /** Every client tick from the game thread: update the state for the check thread, clean up old things. */
    public static void tick() {
        tick++;
        Minecraft mc = Minecraft.getInstance();
        EntityCullingModule m = module();
        if (mc.player == null || mc.level == null || m == null || !m.isActive()) {
            level = null;
            return;
        }
        // from the real camera (also correct in third person and with freecam)
        AioCore core = AioCore.get();
        var cam = core == null ? null : core.platform().camera(1f);
        eye = cam == null ? mc.player.getEyePosition() : new Vec3(cam.x(), cam.y(), cam.z());
        if (sectionsLevel != mc.level) {
            SECTIONS.clear();
            PENDING_SECTIONS.clear();
            sectionsLevel = mc.level;
        }
        level = mc.level;
        firstPerson = mc.options.getCameraType() == CameraType.FIRST_PERSON;
        if (leavesSolid != m.leavesSolid()) SECTIONS.clear();
        leavesSolid = m.leavesSolid();
        traceDistance = m.traceDistance();
        buildSections(mc.level);
        if (tick % 100 == 0) {
            long old = tick - 100;
            ENTITIES.values().removeIf(v -> (v >>> 1) < old);
            BLOCKS.values().removeIf(v -> (v >>> 1) < old);
            if (HIDDEN_STREAK.size() > 4096) HIDDEN_STREAK.clear();
            if (LAST_POINT.size() > 4096) LAST_POINT.clear();
            // forget sections far from the camera
            if (SECTIONS.size() > 6000) SECTIONS.clear();
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

    /** Check thread: process requests, otherwise sleep briefly. */
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
                // World was just rebuilt or similar: again on the next pass
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
                vis = compute(lvl, from, e.getValue(), e.getKey());
            } catch (Throwable t) {
                vis = true; // when in doubt show
            }
            // only hide after two hidden results in a row
            if (vis) HIDDEN_STREAK.remove(e.getKey());
            else if (HIDDEN_STREAK.merge(e.getKey(), 1, Integer::sum) < 2) vis = true;
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

    /** Should the entity be drawn? (distance and occlusion) */
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

    /** Should the entity only tick in a simplified way? (last recognised as hidden) */
    public static boolean skipTick(Entity e) {
        EntityCullingModule m = module();
        if (m == null || !m.cullTicks()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || e == mc.player || e == mc.getCameraEntity() || exempt(e, m)) return false;
        Long v = ENTITIES.get((long) e.getId());
        return v != null && (v & 1) == 0 && tick - (v >>> 1) <= REFRESH_TICKS + 2;
    }

    /** Should the block entity be drawn? */
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

    /** Last result; outdated or unknown → ask the check thread. */
    private static boolean visible(Map<Long, Long> results, Map<Long, AABB> pending, long key, AABB box) {
        Long v = results.get(key);
        if (v == null || tick - (v >>> 1) >= REFRESH_TICKS + (key & 1)) pending.put(key, box);
        return v == null || (v & 1) == 1;
    }

    private static boolean compute(Level lvl, Vec3 from, AABB box, long key) {
        Vec3 center = box.getCenter();
        double dist = from.distanceTo(center);
        if (dist < 4 || dist > traceDistance) return true; // near: always show; far: do not check
        if (box.contains(from)) return true;
        AABB b = box.inflate(0.05);
        double[][] points = {
                {center.x, center.y, center.z},
                {center.x, b.maxY, center.z},
                {b.minX, b.minY, b.minZ}, {b.maxX, b.maxY, b.maxZ},
                {b.minX, b.maxY, b.maxZ}, {b.maxX, b.minY, b.minZ},
                {b.minX, b.maxY, b.minZ}, {b.maxX, b.minY, b.maxZ}};
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        // the point that was visible last time first: usually one ray is enough
        Integer last = LAST_POINT.get(key);
        if (last != null && clear(lvl, cursor, from.x, from.y, from.z, points[last][0], points[last][1], points[last][2])) return true;
        for (int i = 0; i < points.length; i++) {
            if (last != null && i == last) continue;
            double[] p = points[i];
            if (clear(lvl, cursor, from.x, from.y, from.z, p[0], p[1], p[2])) {
                LAST_POINT.put(key, i);
                return true;
            }
        }
        return false;
    }

    private static boolean opaque(BlockState state, Level lvl, BlockPos pos) {
        if (state.isSolidRender()) return true;
        return leavesSolid && state.getBlock() instanceof LeavesBlock;
    }

    /** Ray through the block grid (step by step from block to block); free if no full block is in the way. */
    /** A block or its light changed in this section: rebuild it before it is used again. */
    public static void sectionDirty(int sx, int sy, int sz) {
        long key = SectionPos.asLong(sx, sy, sz);
        if (SECTIONS.remove(key) != null) PENDING_SECTIONS.add(key);
    }

    /** Game thread: build a few requested sections per tick. */
    private static void buildSections(Level lvl) {
        int n = 0;
        for (var it = PENDING_SECTIONS.iterator(); it.hasNext() && n < SECTIONS_PER_TICK; ) {
            long key = it.next();
            it.remove();
            if (buildSection(lvl, key)) n++;
        }
    }

    private static boolean buildSection(Level lvl, long key) {
        int sx = SectionPos.x(key), sy = SectionPos.y(key), sz = SectionPos.z(key);
        if (!lvl.hasChunk(sx, sz)) return false;
        LevelChunk chunk = lvl.getChunk(sx, sz);
        int index = chunk.getSectionIndex(sy << 4);
        LevelChunkSection[] sections = chunk.getSections();
        long[] bits = new long[64];
        if (index >= 0 && index < sections.length && !sections[index].hasOnlyAir()) {
            LevelChunkSection section = sections[index];
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        p.set((sx << 4) + x, (sy << 4) + y, (sz << 4) + z);
                        if (opaque(section.getBlockState(x, y, z), lvl, p)) {
                            int i = (y << 8) | (z << 4) | x;
                            bits[i >> 6] |= 1L << (i & 63);
                        }
                    }
                }
            }
        }
        SECTIONS.put(key, new Section(bits, tick));
        return true;
    }

    /** Check thread: is the block opaque? Unknown sections are requested and count as see-through. */
    private static boolean occludesCached(int x, int y, int z) {
        long key = SectionPos.asLong(x >> 4, y >> 4, z >> 4);
        Section s = SECTIONS.get(key);
        if (s == null) {
            PENDING_SECTIONS.add(key);
            return false;
        }
        int i = ((y & 15) << 8) | ((z & 15) << 4) | (x & 15);
        return (s.bits()[i >> 6] & (1L << (i & 63))) != 0;
    }

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
            if (occludesCached(x, y, z)) return false;
        }
        return true;
    }

    private static int floor(double v) { return (int) Math.floor(v); }
}
