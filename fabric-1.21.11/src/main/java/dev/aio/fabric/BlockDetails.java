package dev.aio.fabric;

import dev.aio.core.platform.LookTarget.Detail;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ComparatorMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Block-specific details for the look-at info. Each row only appears for the block type
 * it belongs to (recognised by the block property, e.g. honey level only for beehives).
 */
final class BlockDetails {
    private static final String[] NOTES = {"Fis", "G", "Gis", "A", "B", "H", "C", "Cis", "D", "Dis", "E", "F"};

    private BlockDetails() {}

    static List<Detail> of(Minecraft client, BlockState state, BlockPos pos) {
        List<Detail> out = new ArrayList<>(3);

        // Bienenstock / Bienennest
        if (state.hasProperty(BeehiveBlock.HONEY_LEVEL)) {
            out.add(Detail.of("Honey", state.getValue(BeehiveBlock.HONEY_LEVEL), 5));
            int bees = beeCount(client, pos);
            if (bees >= 0) out.add(new Detail("Bees", bees + "/3", false));
        }
        // Composter: 0..7 filling, 8 = bone meal ready
        if (state.hasProperty(BlockStateProperties.LEVEL_COMPOSTER)) {
            int level = state.getValue(BlockStateProperties.LEVEL_COMPOSTER);
            out.add(level >= 8 ? new Detail("Level", "fertig", true) : Detail.of("Level", level, 7));
        }
        // Cauldron with water or powder snow
        if (state.hasProperty(BlockStateProperties.LEVEL_CAULDRON)) {
            out.add(Detail.of("Level", state.getValue(BlockStateProperties.LEVEL_CAULDRON), 3));
        }
        // Seelenanker
        if (state.hasProperty(BlockStateProperties.RESPAWN_ANCHOR_CHARGES)) {
            out.add(Detail.of("Charges", state.getValue(BlockStateProperties.RESPAWN_ANCHOR_CHARGES), 4));
        }
        // Ackerboden
        if (state.hasProperty(BlockStateProperties.MOISTURE)) {
            int m = state.getValue(BlockStateProperties.MOISTURE);
            out.add(new Detail("Moisture", m >= 7 ? "nass" : m + "/7", m >= 7));
        }
        // Redstone
        if (state.hasProperty(BlockStateProperties.POWER)) {
            out.add(Detail.of("Redstone power", String.valueOf(state.getValue(BlockStateProperties.POWER))));
        }
        if (state.hasProperty(BlockStateProperties.DELAY)) {
            int delay = state.getValue(BlockStateProperties.DELAY);
            out.add(Detail.of("Delay", delay + (delay == 1 ? " tick" : " ticks")));
        }
        if (state.hasProperty(BlockStateProperties.MODE_COMPARATOR)) {
            boolean compare = state.getValue(BlockStateProperties.MODE_COMPARATOR) == ComparatorMode.COMPARE;
            out.add(Detail.of("Mode", compare ? "Compare" : "Subtract"));
        }
        // Notenblock
        if (state.hasProperty(NoteBlock.NOTE)) {
            int note = state.getValue(NoteBlock.NOTE);
            out.add(Detail.of("Sound", NOTES[note % 12] + " (" + note + ")"));
        }
        // Stackable small blocks
        if (state.hasProperty(BlockStateProperties.PICKLES)) {
            out.add(Detail.of("Sea pickles", state.getValue(BlockStateProperties.PICKLES), 4));
        }
        if (state.hasProperty(BlockStateProperties.CANDLES)) {
            out.add(Detail.of("Candles", state.getValue(BlockStateProperties.CANDLES), 4));
        }
        if (state.hasProperty(BlockStateProperties.EGGS)) {
            out.add(Detail.of("Eggs", state.getValue(BlockStateProperties.EGGS), 4));
        }
        if (state.hasProperty(BlockStateProperties.HATCH)) {
            out.add(Detail.of("Hatching", state.getValue(BlockStateProperties.HATCH), 2));
        }
        if (state.hasProperty(BlockStateProperties.LAYERS)) {
            out.add(Detail.of("Layers", state.getValue(BlockStateProperties.LAYERS), 8));
        }
        return out;
    }

    /** Zuletzt vom Server-Thread gelesene Bienenanzahl je Position. */
    private static final Map<BlockPos, Integer> BEES = new ConcurrentHashMap<>();
    private static volatile BlockPos requested;

    /**
     * Bees in the hive. The server does not send this number to the client, so only in
     * singleplayer. The server world may only be read on the server thread (from the client thread
     * it returns no block entities), so it is queried there and the result cached.
     * Until the first answer is there (one tick), -1 = leave the row out.
     */
    private static int beeCount(Minecraft client, BlockPos pos) {
        var server = client.getSingleplayerServer();
        if (server == null || client.level == null) {
            BEES.clear();
            return -1;
        }
        var dim = client.level.dimension();
        BlockPos key = pos.immutable();
        if (!key.equals(requested)) {
            requested = key;
            if (BEES.size() > 64) BEES.clear();
        }
        server.execute(() -> {
            ServerLevel level = server.getLevel(dim);
            if (level != null && level.getBlockEntity(key) instanceof BeehiveBlockEntity hive) BEES.put(key, hive.getOccupantCount());
        });
        return BEES.getOrDefault(key, -1);
    }
}
