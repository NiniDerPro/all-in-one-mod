package dev.aio.forge;

import dev.aio.core.AioCore;
import dev.aio.core.modules.render.LightOverlayModule;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.aio.forge.shim.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Light Level Overlay, cached per chunk section (16³ blocks).
 * <ul>
 *   <li>Each section is evaluated once. If Minecraft reports a change in it (block
 *       placed/broken or light changed), exactly this section is recomputed on the next tick
 *       – so a torch turns the surroundings green immediately.</li>
 *   <li>New sections are added outwards from the player, with a time budget per tick.</li>
 *   <li>Everything is drawn in one call; the list is only rebuilt on changes.</li>
 * </ul>
 */
final class LightOverlay {
    private LightOverlay() {}

    /** Time budget for evaluating new sections per tick (changed ones always go first). */
    private static final long BUDGET_NANOS = 3_000_000;
    /** Height range around the player in which surfaces are shown. */
    private static final int BELOW = 24, ABOVE = 12;

    /** Section → entries (lx | ly << 4 | lz << 8 | lit << 12) of the floors in the section. */
    private static final Map<Long, int[]> CACHE = new HashMap<>();
    /** Changes reported by Minecraft (can come from several places). */
    private static final Set<Long> DIRTY = ConcurrentHashMap.newKeySet();
    private static ClientLevel cachedLevel;
    private static boolean listDirty = true;
    private static long lastCenter = Long.MIN_VALUE;
    private static int lastRadius = -1;
    /** Zu zeichnende Vierecke: x, y, z, lit – flach. */
    private static int[] quads = new int[0];
    private static int quadCount;

    /** Called when Minecraft wants to redraw a section (block or light change). */
    static void onSectionDirty(int sx, int sy, int sz) {
        if (cachedLevel == null) return;
        DIRTY.add(SectionPos.asLong(sx, sy, sz));
        DIRTY.add(SectionPos.asLong(sx, sy - 1, sz)); // floors at the very top of the section below depend on it
    }

    static void register(AioCore core, Minecraft mc) {
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            LightOverlayModule m = core.module(LightOverlayModule.class);
            if (m == null || !m.isActive() || quadCount == 0 || mc.level == null) return;
            Vec3 cam = context.levelState().cameraRenderState.pos;
            PoseStack pose = context.poseStack();
            pose.pushPose();
            pose.translate(-cam.x, -cam.y, -cam.z);
            int[] q = quads;
            int n = quadCount;
            int danger = m.dangerColor(), safe = m.safeColor();
            boolean showSafe = m.showSafe();
            context.submitNodeCollector().submitCustomGeometry(pose, RenderTypes.debugFilledBox(), (p, buffer) -> {
                for (int i = 0; i < n; i++) {
                    int o = i * 4;
                    boolean lit = q[o + 3] != 0;
                    if (lit && !showSafe) continue;
                    quad(p, buffer, q[o], q[o + 1], q[o + 2], lit ? safe : danger);
                }
            });
            pose.popPose();
        });
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer v, int x, int y, int z, int c) {
        float y1 = y + 0.005f; // just above the surface, no flicker
        float a = 0.06f, x0 = x + a, z0 = z + a, x1 = x + 1 - a, z1 = z + 1 - a;
        v.addVertex(pose, x0, y1, z0).setColor(c);
        v.addVertex(pose, x0, y1, z1).setColor(c);
        v.addVertex(pose, x1, y1, z1).setColor(c);
        v.addVertex(pose, x1, y1, z0).setColor(c);
        // Back side, in case the fill is drawn single-sided
        v.addVertex(pose, x0, y1, z0).setColor(c);
        v.addVertex(pose, x1, y1, z0).setColor(c);
        v.addVertex(pose, x1, y1, z1).setColor(c);
        v.addVertex(pose, x0, y1, z1).setColor(c);
    }

    static void tick(AioCore core, Minecraft mc) {
        LightOverlayModule m = core.module(LightOverlayModule.class);
        ClientLevel level = mc.level;
        if (m == null || !m.isActive() || level == null || mc.player == null) {
            clear();
            return;
        }
        if (level != cachedLevel) {
            clear();
            cachedLevel = level;
        }
        BlockPos center = mc.player.blockPosition();
        int radius = m.radius();
        int csx = SectionPos.blockToSectionCoord(center.getX()), csz = SectionPos.blockToSectionCoord(center.getZ());
        int syMin = Math.max(level.getMinSectionY(), SectionPos.blockToSectionCoord(center.getY() - BELOW));
        int syMax = Math.min(level.getMaxSectionY(), SectionPos.blockToSectionCoord(center.getY() + ABOVE));
        int rc = (radius >> 4) + 1;

        // 1. Recompute changed sections in range immediately
        if (!DIRTY.isEmpty()) {
            for (Iterator<Long> it = DIRTY.iterator(); it.hasNext(); ) {
                long key = it.next();
                it.remove();
                if (CACHE.containsKey(key)) {
                    CACHE.put(key, scan(level, SectionPos.x(key), SectionPos.y(key), SectionPos.z(key)));
                    listDirty = true;
                }
            }
        }

        // 2. Add missing sections from the inside out (time budget)
        long start = System.nanoTime();
        outer:
        for (int ring = 0; ring <= rc; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                    if (dx * dx + dz * dz > rc * rc) continue;
                    int sx = csx + dx, sz = csz + dz;
                    for (int sy = syMin; sy <= syMax; sy++) {
                        long key = SectionPos.asLong(sx, sy, sz);
                        if (CACHE.containsKey(key)) continue;
                        if (level.getChunkSource().getChunk(sx, sz, ChunkStatus.FULL, false) == null) continue;
                        CACHE.put(key, scan(level, sx, sy, sz));
                        listDirty = true;
                        if (System.nanoTime() - start > BUDGET_NANOS) break outer;
                    }
                }
            }
        }

        // 3. Weit entfernte Abschnitte vergessen
        if (CACHE.size() > 64) {
            CACHE.keySet().removeIf(key -> {
                int dx = SectionPos.x(key) - csx, dz = SectionPos.z(key) - csz, sy = SectionPos.y(key);
                boolean out = dx * dx + dz * dz > (rc + 2) * (rc + 2) || sy < syMin - 1 || sy > syMax + 1;
                if (out) listDirty = true;
                return out;
            });
        }

        long centerKey = center.asLong();
        if (listDirty || centerKey != lastCenter || radius != lastRadius) {
            rebuildList(center, radius);
            lastCenter = centerKey;
            lastRadius = radius;
            listDirty = false;
        }
    }

    private static void rebuildList(BlockPos center, int radius) {
        int r2 = radius * radius;
        int yMin = center.getY() - BELOW, yMax = center.getY() + ABOVE;
        List<int[]> parts = new ArrayList<>();
        int total = 0;
        int[] buf = new int[4096 * 4];
        int n = 0;
        for (Map.Entry<Long, int[]> e : CACHE.entrySet()) {
            long key = e.getKey();
            int bx = SectionPos.sectionToBlockCoord(SectionPos.x(key)), by = SectionPos.sectionToBlockCoord(SectionPos.y(key)),
                    bz = SectionPos.sectionToBlockCoord(SectionPos.z(key));
            for (int entry : e.getValue()) {
                int x = bx + (entry & 15), y = by + ((entry >> 4) & 15) + 1, z = bz + ((entry >> 8) & 15);
                if (y < yMin || y > yMax) continue;
                int dx = x - center.getX(), dz = z - center.getZ();
                if (dx * dx + dz * dz > r2) continue;
                if (n + 4 > buf.length) {
                    parts.add(buf);
                    total += n;
                    buf = new int[buf.length];
                    n = 0;
                }
                buf[n] = x;
                buf[n + 1] = y;
                buf[n + 2] = z;
                buf[n + 3] = (entry >> 12) & 1;
                n += 4;
            }
        }
        int[] out = new int[total + n];
        int pos = 0;
        for (int[] p : parts) {
            System.arraycopy(p, 0, out, pos, p.length);
            pos += p.length;
        }
        System.arraycopy(buf, 0, out, pos, n);
        quads = out;
        quadCount = out.length / 4;
    }

    /** All spawn floors of a section with block light status. */
    private static int[] scan(ClientLevel level, int sx, int sy, int sz) {
        LevelChunk chunk = level.getChunkSource().getChunk(sx, sz, ChunkStatus.FULL, false);
        if (chunk == null) return new int[0];
        int index = level.getSectionIndexFromSectionY(sy);
        if (index < 0 || index >= chunk.getSections().length) return new int[0];
        LevelChunkSection section = chunk.getSection(index);
        if (section.hasOnlyAir()) return new int[0];
        int bx = SectionPos.sectionToBlockCoord(sx), by = SectionPos.sectionToBlockCoord(sy), bz = SectionPos.sectionToBlockCoord(sz);
        BlockPos.MutableBlockPos floor = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();
        int[] buf = new int[64];
        int n = 0;
        for (int ly = 0; ly < 16; ly++) {
            for (int lz = 0; lz < 16; lz++) {
                for (int lx = 0; lx < 16; lx++) {
                    BlockState fs = section.getBlockState(lx, ly, lz);
                    if (fs.isAir()) continue;
                    floor.set(bx + lx, by + ly, bz + lz);
                    if (!fs.isFaceSturdy(level, floor, Direction.UP)) continue;
                    above.set(floor.getX(), floor.getY() + 1, floor.getZ());
                    BlockState a1 = ly < 15 ? section.getBlockState(lx, ly + 1, lz) : level.getBlockState(above);
                    if (!NaturalSpawner.isValidEmptySpawnBlock(level, above, a1, a1.getFluidState(), EntityTypes.ZOMBIE)) continue;
                    if (!fs.isValidSpawn(level, floor, EntityTypes.ZOMBIE)) continue;
                    above.set(floor.getX(), floor.getY() + 2, floor.getZ());
                    BlockState a2 = ly < 14 ? section.getBlockState(lx, ly + 2, lz) : level.getBlockState(above);
                    if (!NaturalSpawner.isValidEmptySpawnBlock(level, above, a2, a2.getFluidState(), EntityTypes.ZOMBIE)) continue;
                    above.set(floor.getX(), floor.getY() + 1, floor.getZ());
                    boolean lit = level.getBrightness(LightLayer.BLOCK, above) > 0; // Sonne ignorieren
                    if (n == buf.length) buf = Arrays.copyOf(buf, n * 2);
                    buf[n++] = lx | ly << 4 | lz << 8 | (lit ? 1 << 12 : 0);
                }
            }
        }
        return Arrays.copyOf(buf, n);
    }

    private static void clear() {
        CACHE.clear();
        DIRTY.clear();
        cachedLevel = null;
        quads = new int[0];
        quadCount = 0;
        lastCenter = Long.MIN_VALUE;
        listDirty = true;
    }
}
