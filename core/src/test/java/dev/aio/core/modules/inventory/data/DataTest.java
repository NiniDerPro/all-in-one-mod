package dev.aio.core.modules.inventory.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Evaluation of loot tables, trades and world generation in the 26.3 format (like the vanilla data). */
class DataTest {
    private static JsonElement j(String s) { return JsonParser.parseString(s); }

    @Test void lootTableNewFormat() {
        // like entities/zombie: pool 1 always (0–2 flesh), pool 2 only from the player with 2.5 %
        Map<String, JsonElement> tables = Map.of("minecraft:entities/zombie", j("""
                {"type":"minecraft:entity","pools":[
                  {"rolls":1,"entries":[{"type":"minecraft:item","name":"minecraft:rotten_flesh",
                     "modifier":[{"type":"minecraft:set_count","count":{"type":"minecraft:uniform","min":0,"max":2}}]}]},
                  {"rolls":1,"condition":[{"type":"minecraft:killed_by_player"},
                     {"type":"minecraft:random_chance_with_enchanted_bonus","unenchanted_chance":0.025}],
                   "entries":[{"type":"minecraft:item","name":"minecraft:iron_ingot"},{"type":"minecraft:item","name":"minecraft:carrot"}]}
                ]}"""));
        List<LootAnalyzer.Drop> drops = new LootAnalyzer(tables).analyze("minecraft:entities/zombie");
        LootAnalyzer.Drop flesh = drops.stream().filter(d -> d.item().equals("minecraft:rotten_flesh")).findFirst().orElseThrow();
        assertEquals(1.0, flesh.chance(), 1e-9);
        assertEquals("0–2", flesh.countText());
        LootAnalyzer.Drop iron = drops.stream().filter(d -> d.item().equals("minecraft:iron_ingot")).findFirst().orElseThrow();
        assertEquals(0.0125, iron.chance(), 1e-9, "2,5 % Pool-Chance, halbes Gewicht");
        assertTrue(iron.conditional(), "only when killed by the player");
    }

    @Test void chestWithWeightsRollsAndNestedTable() {
        Map<String, JsonElement> tables = Map.of(
                "minecraft:chests/test", j("""
                {"pools":[{"rolls":{"type":"minecraft:uniform","min":2,"max":4},"entries":[
                   {"type":"minecraft:item","name":"minecraft:diamond","weight":1},
                   {"type":"minecraft:empty","weight":3},
                   {"type":"minecraft:loot_table","value":"minecraft:chests/inner","weight":1}]}]}"""),
                "minecraft:chests/inner", j("""
                {"pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":"minecraft:emerald",
                   "functions":[{"function":"minecraft:set_count","count":3}]}]}]}"""));
        List<LootAnalyzer.Drop> drops = new LootAnalyzer(tables).analyze("minecraft:chests/test");
        double expected = 1 - Math.pow(1 - 0.2, 3); // 3 rolls on average, 1/5 weight
        assertEquals(expected, drops.stream().filter(d -> d.item().equals("minecraft:diamond")).findFirst().orElseThrow().chance(), 1e-9);
        LootAnalyzer.Drop emerald = drops.stream().filter(d -> d.item().equals("minecraft:emerald")).findFirst().orElseThrow();
        assertEquals(expected, emerald.chance(), 1e-9, "verschachtelte Tabelle anteilig");
        assertEquals("3", emerald.countText(), "old format (functions) works too");
    }

    @Test void tradesResolveTagsAndCounts() {
        TradeData data = new TradeData(
                Map.of("minecraft:armorer/level_1", j("{\"amount\":2,\"trades\":\"#minecraft:armorer/level_1\"}")),
                Map.of("minecraft:armorer/1/emerald_iron_boots", j("""
                        {"gives":{"id":"minecraft:iron_boots"},"max_uses":12,"wants":{"count":4,"id":"minecraft:emerald"}}"""),
                        "minecraft:smith/1/coal_emerald", j("""
                        {"gives":{"id":"minecraft:emerald"},"wants":{"count":{"type":"minecraft:uniform","min":10,"max":15},"id":"minecraft:coal"},"xp":2}""")),
                Map.of("minecraft:armorer/level_1", j("{\"values\":[\"#minecraft:common_smith/level_1\",\"minecraft:armorer/1/emerald_iron_boots\"]}"),
                        "minecraft:common_smith/level_1", j("{\"values\":[\"minecraft:smith/1/coal_emerald\"]}")));
        TradeData.TradeSet set = data.set("minecraft:armorer/level_1");
        assertEquals(2, set.amount());
        assertEquals(2, set.trades().size());
        TradeData.Trade coal = set.trades().get(0);
        assertEquals("minecraft:coal", coal.wants().item());
        assertEquals("10–15", coal.wants().countText());
        assertEquals(0, coal.wants().iconCount(), "range: no fixed number on the icon");
        assertEquals(4, set.trades().get(1).wants().iconCount());
    }

    @Test void worldGenSpawnsAndOres() {
        WorldGenData wg = new WorldGenData(
                Map.of("minecraft:plains", j("""
                        {"attributes":{"minecraft:gameplay/natural_mob_spawns":{"argument":{"spawns_by_category":{
                          "monster":[{"type":"minecraft:zombie","count":4,"weight":90}]}}}},
                         "features":[[],["minecraft:ore_iron_upper"]]}"""),
                        "minecraft:nether_wastes", j("{\"features\":[[\"minecraft:ore_quartz_nether\"]]}")),
                Map.of("minecraft:ore_iron_upper", j("""
                        {"feature":"minecraft:ore_iron","placement":[{"type":"minecraft:count","count":90},
                          {"type":"minecraft:height_range","height":{"type":"minecraft:trapezoid",
                            "min_inclusive":{"absolute":80},"max_inclusive":{"absolute":384}}}]}"""),
                        "minecraft:ore_quartz_nether", j("""
                        {"feature":"minecraft:ore_quartz","placement":[{"type":"minecraft:height_range","height":{"type":"minecraft:uniform",
                          "min_inclusive":{"above_bottom":10},"max_inclusive":{"below_top":10}}}]}""")),
                Map.of("minecraft:ore_iron", j("{\"type\":\"minecraft:ore\",\"targets\":[{\"state\":\"minecraft:iron_ore\"}]}"),
                        "minecraft:ore_quartz", j("{\"type\":\"minecraft:ore\",\"config\":{\"targets\":[{\"state\":{\"Name\":\"minecraft:nether_quartz_ore\"}}]}}")),
                Map.of("minecraft:is_nether", j("{\"values\":[\"minecraft:nether_wastes\"]}")),
                Map.of("minecraft:fortress", j("""
                        {"spawn_overrides":{"monster":{"spawns":[{"type":"minecraft:blaze","count":{"type":"minecraft:uniform","min_inclusive":2,"max_inclusive":3},"weight":10}]}}}""")));
        WorldGenData.Spawn zombie = wg.spawnsOf("minecraft:zombie").get(0);
        assertEquals("minecraft:plains", zombie.place());
        assertEquals(90, zombie.weight());
        WorldGenData.Spawn blaze = wg.spawnsOf("minecraft:blaze").get(0);
        assertTrue(blaze.structure());
        assertEquals(2, blaze.minCount());
        assertEquals(3, blaze.maxCount());
        WorldGenData.Ore iron = wg.oresOf("minecraft:iron_ore").get(0);
        assertEquals(80, iron.minY());
        assertEquals(319, iron.maxY(), "limited to the world height");
        assertEquals(90, iron.perChunk());
        WorldGenData.Ore quartz = wg.oresOf("minecraft:nether_quartz_ore").get(0);
        assertEquals(WorldGenData.Dimension.NETHER, quartz.dimension());
        assertEquals(10, quartz.minY());
        assertEquals(245, quartz.maxY());
    }
}
