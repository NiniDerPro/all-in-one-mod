package dev.aio.core.modules.inventory.data;

import dev.aio.core.platform.Platform;
import dev.aio.core.platform.Profession;

import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Alles, was der Item-Browser über die Welt weiß: Beute (Truhen, Mobs, Angeln, Piglin-Tausch, …),
 * Handel je Beruf, Spawns und Erzvorkommen. Wird einmal pro Welt im Hintergrund geladen.
 */
public final class WorldInfo {
    /** Woher ein Item kommt: Loot Table und Chance. */
    public record Source(String table, LootAnalyzer.Drop drop) {}

    /** Ein Handel samt Herkunft. {@code profession} null = wandernder Händler. */
    public record TradeEntry(Profession profession, int level, String rarity, TradeData.Trade trade) {}

    public static final String BARTERING = "minecraft:gameplay/piglin_bartering";

    final Map<String, List<Source>> sources = new HashMap<>();
    final Map<String, List<LootAnalyzer.Drop>> dropsByTable = new HashMap<>();
    final List<TradeEntry> trades = new ArrayList<>();
    final List<Profession> professions;
    final WorldGenData worldGen;
    private final long loadMillis;

    private WorldInfo(Platform p) {
        long start = System.currentTimeMillis();
        Map<String, JsonElement> loot = p.dataFiles("loot_table");
        LootAnalyzer analyzer = new LootAnalyzer(loot);
        for (String table : loot.keySet()) {
            // Blöcke droppen sich meist selbst: das zeigt jede Rezeptansicht ohnehin nicht sinnvoll an
            if (table.contains(":blocks/")) continue;
            List<LootAnalyzer.Drop> drops = analyzer.analyze(table);
            if (drops.isEmpty()) continue;
            dropsByTable.put(table, drops);
            for (LootAnalyzer.Drop d : drops) sources.computeIfAbsent(d.item(), k -> new ArrayList<>()).add(new Source(table, d));
        }
        for (List<Source> list : sources.values()) list.sort((a, b) -> Double.compare(b.drop().chance(), a.drop().chance()));

        TradeData tradeData = new TradeData(p.dataFiles("trade_set"), p.dataFiles("villager_trade"), p.dataFiles("tags/villager_trade"));
        professions = List.copyOf(p.professions());
        for (Profession prof : professions) {
            for (Map.Entry<Integer, String> lvl : new TreeMap<>(prof.tradeSets()).entrySet()) {
                for (TradeData.Trade t : tradeData.set(lvl.getValue()).trades()) trades.add(new TradeEntry(prof, lvl.getKey(), "", t));
            }
        }
        String[][] wandering = {{"minecraft:wandering_trader/buying", "buys"}, {"minecraft:wandering_trader/common", "common"},
                {"minecraft:wandering_trader/uncommon", "rare"}};
        for (String[] w : wandering) {
            for (TradeData.Trade t : tradeData.set(w[0]).trades()) trades.add(new TradeEntry(null, 0, w[1], t));
        }

        Map<String, JsonElement> features = new HashMap<>(p.dataFiles("worldgen/configured_feature"));
        features.putAll(p.dataFiles("worldgen/feature"));
        worldGen = new WorldGenData(p.dataFiles("worldgen/biome"), p.dataFiles("worldgen/placed_feature"), features,
                p.dataFiles("tags/worldgen/biome"), p.dataFiles("worldgen/structure"));
        loadMillis = System.currentTimeMillis() - start;
    }

    /** Lädt alles (blockiert, für den Hintergrund-Thread). */
    public static WorldInfo load(Platform p) { return new WorldInfo(p); }

    public long loadMillis() { return loadMillis; }

    public List<Source> sourcesOf(String item) { return sources.getOrDefault(item, List.of()); }

    public List<LootAnalyzer.Drop> drops(String table) { return dropsByTable.getOrDefault(table, List.of()); }

    public List<TradeEntry> trades() { return trades; }

    public List<Profession> professions() { return professions; }

    public WorldGenData worldGen() { return worldGen; }
}
