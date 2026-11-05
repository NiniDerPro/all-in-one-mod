package dev.aio.fabric;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.aio.core.AioCore;
import dev.aio.core.modules.render.LightOverlayModule;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Light Level Overlay. Damit es nicht laggt:
 * <ul>
 *   <li>Gescannt wird nur, wenn man sich um einen Block bewegt hat oder spätestens jede Sekunde
 *       (Fackel gesetzt, Block abgebaut), und dann verteilt auf mehrere Ticks.</li>
 *   <li>Pro Spalte nur die Oberflächen im Höhenbereich um den Spieler.</li>
 *   <li>Gezeichnet wird alles in einem einzigen Aufruf als flache Vierecke.</li>
 * </ul>
 */
final class LightOverlay {
    private LightOverlay() {}

    /** Ergebnis: x, y (Oberkante), z, Farbe – flach in einem Array. */
    private static int[] quads = new int[0];
    private static int quadCount;
    // Scan in Arbeit
    private static int[] building = new int[4 * 4096];
    private static int buildCount;
    private static int scanRow = Integer.MIN_VALUE, scanCx, scanCy, scanCz, scanR;
    private static BlockPos lastCenter;
    private static int ticksSinceScan;
    private static final int ROWS_PER_TICK = 8;

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
            context.submitNodeCollector().submitCustomGeometry(pose, RenderTypes.debugFilledBox(), (p, buffer) -> {
                for (int i = 0; i < n; i++) {
                    int o = i * 4;
                    quad(p, buffer, q[o], q[o + 1], q[o + 2], q[o + 3]);
                }
            });
            pose.popPose();
        });
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer v, int x, int y, int z, int c) {
        float y1 = y + 0.005f; // knapp über der Oberfläche, kein Flimmern
        float a = 0.06f, x0 = x + a, z0 = z + a, x1 = x + 1 - a, z1 = z + 1 - a;
        v.addVertex(pose, x0, y1, z0).setColor(c);
        v.addVertex(pose, x0, y1, z1).setColor(c);
        v.addVertex(pose, x1, y1, z1).setColor(c);
        v.addVertex(pose, x1, y1, z0).setColor(c);
        // Rückseite, falls die Füllung einseitig gezeichnet wird
        v.addVertex(pose, x0, y1, z0).setColor(c);
        v.addVertex(pose, x1, y1, z0).setColor(c);
        v.addVertex(pose, x1, y1, z1).setColor(c);
        v.addVertex(pose, x0, y1, z1).setColor(c);
    }

    /** Jeden Client-Tick: bei Bedarf Scan starten bzw. fortsetzen. */
    static void tick(AioCore core, Minecraft mc) {
        LightOverlayModule m = core.module(LightOverlayModule.class);
        ClientLevel level = mc.level;
        if (m == null || !m.isActive() || level == null || mc.player == null) {
            quadCount = 0;
            lastCenter = null;
            scanRow = Integer.MIN_VALUE;
            return;
        }
        BlockPos center = mc.player.blockPosition();
        ticksSinceScan++;
        if (scanRow == Integer.MIN_VALUE && (lastCenter == null || !center.equals(lastCenter) || ticksSinceScan >= 20)) {
            lastCenter = center;
            ticksSinceScan = 0;
            scanR = m.radius();
            scanCx = center.getX();
            scanCy = center.getY();
            scanCz = center.getZ();
            scanRow = -scanR;
            buildCount = 0;
        }
        if (scanRow == Integer.MIN_VALUE) return;
        int danger = m.dangerColor(), safe = m.safeColor();
        boolean showSafe = m.showSafe();
        BlockPos.MutableBlockPos floor = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();
        int r2 = scanR * scanR;
        // Höhenbereich: unter dem Spieler mehr, darüber weniger
        int yMin = Math.max(level.getMinY(), scanCy - 12), yMax = Math.min(level.getMaxY() - 2, scanCy + 6);
        for (int k = 0; k < ROWS_PER_TICK && scanRow <= scanR; k++, scanRow++) {
            int dx = scanRow;
            for (int dz = -scanR; dz <= scanR; dz++) {
                if (dx * dx + dz * dz > r2) continue;
                int x = scanCx + dx, z = scanCz + dz;
                if (!level.hasChunk(x >> 4, z >> 4)) continue;
                for (int y = yMax; y >= yMin; y--) {
                    floor.set(x, y, z);
                    BlockState fs = level.getBlockState(floor);
                    if (fs.isAir() || !fs.isFaceSturdy(level, floor, Direction.UP)) continue;
                    above.set(x, y + 1, z);
                    BlockState as = level.getBlockState(above);
                    if (!NaturalSpawner.isValidEmptySpawnBlock(level, above, as, as.getFluidState(), EntityTypes.ZOMBIE)) continue;
                    if (!fs.isValidSpawn(level, floor, EntityTypes.ZOMBIE)) continue;
                    above.set(x, y + 2, z);
                    BlockState as2 = level.getBlockState(above);
                    if (!NaturalSpawner.isValidEmptySpawnBlock(level, above, as2, as2.getFluidState(), EntityTypes.ZOMBIE)) continue;
                    above.set(x, y + 1, z);
                    int light = level.getBrightness(LightLayer.BLOCK, above); // Sonne ignorieren
                    if (light > 0 && !showSafe) continue;
                    add(x, y + 1, z, light == 0 ? danger : safe);
                }
            }
        }
        if (scanRow > scanR) {
            // fertig: Ergebnis übernehmen
            int[] done = new int[buildCount * 4];
            System.arraycopy(building, 0, done, 0, done.length);
            quads = done;
            quadCount = buildCount;
            scanRow = Integer.MIN_VALUE;
        }
    }

    private static void add(int x, int y, int z, int color) {
        if ((buildCount + 1) * 4 > building.length) {
            int[] bigger = new int[building.length * 2];
            System.arraycopy(building, 0, bigger, 0, building.length);
            building = bigger;
        }
        int o = buildCount * 4;
        building[o] = x;
        building[o + 1] = y;
        building[o + 2] = z;
        building[o + 3] = color;
        buildCount++;
    }
}
