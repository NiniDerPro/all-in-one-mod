package dev.aio.fabric;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

/** Liest für die Karte Farbe und Höhe der obersten sichtbaren Blöcke einer Säule. */
final class MapSampler {
    private MapSampler() {}

    private static final BlockPos.MutableBlockPos POS = new BlockPos.MutableBlockPos();

    static long column(Minecraft mc, int x, int z, int refY) {
        ClientLevel level = mc.level;
        if (level == null) return 0;
        LevelChunk chunk = level.getChunkSource().getChunk(x >> 4, z >> 4, ChunkStatus.FULL, false);
        if (chunk == null) return 0;
        int y;
        if (level.dimensionType().hasCeiling()) {
            // Nether: unter dem Dach von der Spielerhöhe nach unten zur ersten Oberfläche
            y = Math.min(level.getMaxY() - 1, refY + 4);
            while (y > level.getMinY() && !(solid(level, x, y, z) && !solid(level, x, y + 1, z))) y--;
        } else {
            y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15) - 1;
        }
        if (y < level.getMinY()) return 0;
        POS.set(x, y, z);
        BlockState state = level.getBlockState(POS);
        int rgb = state.getMapColor(level, POS).col;
        if (rgb == 0) { // Luft/durchsichtig: den Block darunter nehmen
            for (int i = 0; i < 8 && rgb == 0 && y > level.getMinY(); i++) {
                POS.set(x, --y, z);
                state = level.getBlockState(POS);
                rgb = state.getMapColor(level, POS).col;
            }
        }
        if (!state.getFluidState().isEmpty()) {
            // Wasser wird mit der Tiefe dunkler
            int depth = 0;
            while (depth < 12 && y - depth > level.getMinY() && !level.getBlockState(POS.set(x, y - depth - 1, z)).getFluidState().isEmpty()) depth++;
            float k = 1f - Math.min(depth, 12) / 16f;
            rgb = ((int) (((rgb >> 16) & 255) * k) << 16) | ((int) (((rgb >> 8) & 255) * k) << 8) | (int) ((rgb & 255) * k);
        }
        return ((long) (y + 1024) << 32) | (0xFF000000L | rgb) & 0xFFFFFFFFL;
    }

    private static boolean solid(ClientLevel level, int x, int y, int z) {
        POS.set(x, y, z);
        return !level.getBlockState(POS).getCollisionShape(level, POS).isEmpty();
    }
}
