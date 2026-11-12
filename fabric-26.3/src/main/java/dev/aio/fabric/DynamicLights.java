package dev.aio.fabric;

import dev.aio.core.AioCore;
import dev.aio.core.modules.render.DynamicLightsModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dynamic Lights. On every update the light sources are collected and published as an immutable list
 * (the chunk build threads read it concurrently). If a source moved by a
 * block or its strength changed, only the chunk sections in its range are rebuilt
 * – old and new position.
 */
public final class DynamicLights {
    private DynamicLights() {}

    /** A light source: centre and strength 1..15. */
    private record Source(double x, double y, double z, int level, long key) {}

    private static volatile Source[] sources = new Source[0];
    private static volatile boolean enabled;
    private static volatile boolean entities;
    /** Last built sources (block + strength), to rebuild only on change. */
    private static Map<Integer, long[]> last = new HashMap<>();
    private static int tickCounter;

    // ---- Abfrage (beliebiger Thread) ----

    /** Dynamisches Blocklicht an einer Position (0 = keins). */
    public static int at(double x, double y, double z) {
        Source[] s = sources;
        if (!enabled || s.length == 0) return 0;
        int best = 0;
        for (Source src : s) {
            double dx = x - src.x, dy = y - src.y, dz = z - src.z;
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 >= src.level * src.level) continue;
            int l = (int) Math.round(src.level - Math.sqrt(d2));
            if (l > best) {
                best = l;
                if (best >= 15) break;
            }
        }
        return Math.min(15, best);
    }

    public static int atBlock(BlockPos pos) { return at(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5); }

    public static boolean lightsEntities() { return enabled && entities; }

    // ---- Leuchtkraft von Items ----

    public static int luminance(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        if (stack.is(Items.LAVA_BUCKET)) return 15;
        if (stack.is(Items.BLAZE_ROD) || stack.is(Items.BLAZE_POWDER)) return 10;
        if (stack.is(Items.GLOWSTONE_DUST) || stack.is(Items.GLOW_INK_SAC) || stack.is(Items.GLOW_BERRIES)) return 8;
        if (stack.is(Items.NETHER_STAR)) return 12;
        if (stack.is(Items.MAGMA_CREAM) || stack.is(Items.SPECTRAL_ARROW) || stack.is(Items.PRISMARINE_CRYSTALS)) return 6;
        if (stack.getItem() instanceof BlockItem bi) return bi.getBlock().defaultBlockState().getLightEmission();
        return 0;
    }

    // ---- Update (Render-Thread, jeden Tick) ----

    public static void tick(Minecraft mc) {
        AioCore core = AioCore.get();
        DynamicLightsModule m = core == null ? null : core.module(DynamicLightsModule.class);
        ClientLevel level = mc.level;
        boolean on = m != null && m.isActive() && level != null && mc.player != null;
        if (!on) {
            if (enabled) {
                enabled = false;
                Source[] old = sources;
                sources = new Source[0];
                if (level != null) for (Source s : old) dirty(level, s.x, s.y, s.z, s.level);
                last = new HashMap<>();
            }
            return;
        }
        enabled = true;
        entities = m.entitiesLit();
        if (++tickCounter % m.updateEvery() != 0) return;

        List<Source> list = new ArrayList<>();
        DynamicLightsModule.Mode mode = m.mode();
        Entity view = mc.getCameraEntity() == null ? mc.player : mc.getCameraEntity();
        if (mode == DynamicLightsModule.Mode.SELF) {
            add(list, mc.player);
        } else {
            for (Entity e : level.entitiesForRendering()) {
                if (e.distanceToSqr(view) > 64 * 64) continue;
                if (mode == DynamicLightsModule.Mode.PLAYERS && !(e instanceof Player)) continue;
                add(list, e);
            }
        }
        Source[] next = list.toArray(new Source[0]);

        // Only rebuild changed sources
        Map<Integer, long[]> now = new HashMap<>();
        for (Source s : next) {
            long block = BlockPos.asLong((int) Math.floor(s.x), (int) Math.floor(s.y), (int) Math.floor(s.z));
            int id = (int) s.key;
            now.put(id, new long[]{block, s.level});
            long[] prev = last.get(id);
            if (prev == null) {
                dirty(level, s.x, s.y, s.z, s.level);
            } else if (prev[0] != block || prev[1] != s.level) {
                // old and new range as one common area (usually almost identical)
                BlockPos p = BlockPos.of(prev[0]);
                int r = Math.max(s.level, (int) prev[1]);
                dirtyRange(level, Math.min(p.getX(), s.x) - r, Math.min(p.getY(), s.y) - r, Math.min(p.getZ(), s.z) - r,
                        Math.max(p.getX() + 1, s.x) + r, Math.max(p.getY() + 1, s.y) + r, Math.max(p.getZ() + 1, s.z) + r);
            }
        }
        for (Map.Entry<Integer, long[]> e : last.entrySet()) {
            if (!now.containsKey(e.getKey())) {
                BlockPos p = BlockPos.of(e.getValue()[0]);
                dirty(level, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, (int) e.getValue()[1]);
            }
        }
        last = now;
        sources = next;
    }

    private static void add(List<Source> list, Entity e) {
        int lum = 0;
        if (e instanceof LivingEntity le) {
            lum = Math.max(luminance(le.getMainHandItem()), luminance(le.getOffhandItem()));
        } else if (e instanceof ItemEntity item) {
            lum = luminance(item.getItem());
        }
        if (e.isOnFire()) lum = 15;
        if (lum <= 0) return;
        list.add(new Source(e.getX(), e.getEyeY() - 0.2, e.getZ(), lum, e.getId()));
    }

    /** Have the chunk sections in range of a source rebuilt. */
    private static void dirty(ClientLevel level, double x, double y, double z, int lum) {
        int r = Math.max(1, lum);
        dirtyRange(level, x - r, y - r, z - r, x + r, y + r, z + r);
    }

    private static void dirtyRange(ClientLevel level, double x1, double y1, double z1, double x2, double y2, double z2) {
        level.setSectionRangeDirty(
                SectionPos.blockToSectionCoord((int) Math.floor(x1)), SectionPos.blockToSectionCoord((int) Math.floor(y1)),
                SectionPos.blockToSectionCoord((int) Math.floor(z1)), SectionPos.blockToSectionCoord((int) Math.floor(x2)),
                SectionPos.blockToSectionCoord((int) Math.floor(y2)), SectionPos.blockToSectionCoord((int) Math.floor(z2)));
    }
}
