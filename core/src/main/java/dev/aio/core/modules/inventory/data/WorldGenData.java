package dev.aio.core.modules.inventory.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * World generation from the data packs: which mobs spawn in which biomes and structures and
 * where which ores generate (with height range and frequency).
 */
public final class WorldGenData {
    public enum Dimension { OVERWORLD(-64, 319), NETHER(0, 255), END(0, 255);
        final int minY, maxY;
        Dimension(int minY, int maxY) { this.minY = minY; this.maxY = maxY; }
    }

    /** Spawn of a mob in a biome or structure. */
    public record Spawn(String place, boolean structure, String category, int weight, double minCount, double maxCount) {}

    /** An ore deposit (one placed feature). */
    public record Ore(String feature, int minY, int maxY, String shape, double perChunk, List<String> biomes, Dimension dimension) {}

    private final Map<String, List<Spawn>> spawns = new HashMap<>();
    private final Map<String, List<Ore>> ores = new HashMap<>();
    private final Map<String, String> structureBiomeTags = new LinkedHashMap<>();

    /**
     * @param biomes     worldgen/biome
     * @param placed     worldgen/placed_feature
     * @param features   worldgen/feature (26.x) or worldgen/configured_feature (older)
     * @param biomeTags  tags/worldgen/biome
     * @param structures worldgen/structure
     */
    public WorldGenData(Map<String, JsonElement> biomes, Map<String, JsonElement> placed, Map<String, JsonElement> features,
                        Map<String, JsonElement> biomeTags, Map<String, JsonElement> structures) {
        Set<String> nether = tagMembers(biomeTags, "minecraft:is_nether", 0);
        Set<String> end = tagMembers(biomeTags, "minecraft:is_end", 0);

        Map<String, List<String>> biomesByPlaced = new LinkedHashMap<>();
        Map<String, Dimension> dimByPlaced = new HashMap<>();
        for (Map.Entry<String, JsonElement> b : biomes.entrySet()) {
            String biome = b.getKey();
            JsonObject o = Json.obj(b.getValue());
            if (o == null) continue;
            readSpawns(biome, false, spawnTable(o));
            Dimension dim = nether.contains(biome) ? Dimension.NETHER : end.contains(biome) ? Dimension.END : Dimension.OVERWORLD;
            for (JsonElement step : Json.list(o, "features")) {
                if (!step.isJsonArray()) continue;
                for (JsonElement f : step.getAsJsonArray()) {
                    if (!f.isJsonPrimitive()) continue;
                    String id = Json.id(f.getAsString());
                    biomesByPlaced.computeIfAbsent(id, k -> new ArrayList<>()).add(biome);
                    dimByPlaced.putIfAbsent(id, dim);
                }
            }
        }

        for (Map.Entry<String, List<String>> e : biomesByPlaced.entrySet()) {
            JsonObject pf = Json.obj(placed.get(e.getKey()));
            if (pf == null) continue;
            JsonElement featureRef = pf.get("feature");
            JsonObject feature = featureRef != null && featureRef.isJsonPrimitive()
                    ? Json.obj(features.get(Json.id(featureRef.getAsString()))) : Json.obj(featureRef);
            List<String> blocks = oreTargets(feature);
            if (blocks.isEmpty()) continue;
            Dimension dim = dimByPlaced.getOrDefault(e.getKey(), Dimension.OVERWORLD);
            int minY = dim.minY, maxY = dim.maxY;
            String shape = "";
            double perChunk = 1;
            for (JsonElement m : Json.list(pf, "placement")) {
                JsonObject mod = Json.obj(m);
                if (mod == null) continue;
                String type = Json.id(Json.str(mod, "type"));
                switch (type) {
                    case "minecraft:height_range" -> {
                        JsonObject h = Json.obj(mod, "height");
                        if (h == null) break;
                        minY = anchor(Json.obj(h, "min_inclusive"), dim, dim.minY);
                        maxY = anchor(Json.obj(h, "max_inclusive"), dim, dim.maxY);
                        shape = Json.id(Json.str(h, "type")).replace("minecraft:", "");
                    }
                    case "minecraft:count" -> perChunk *= Json.range(mod.get("count"), Json.Range.ONE).avg();
                    case "minecraft:rarity_filter" -> {
                        double c = Json.range(mod.get("chance"), Json.Range.ONE).avg();
                        if (c > 0) perChunk /= c;
                    }
                    default -> { }
                }
            }
            // Trapezoid distributions often reach below/above the world: limit to the real range
            minY = Math.max(dim.minY, minY);
            maxY = Math.min(dim.maxY, maxY);
            Ore ore = new Ore(e.getKey(), minY, maxY, shape, perChunk, List.copyOf(e.getValue()), dim);
            for (String block : blocks) ores.computeIfAbsent(block, k -> new ArrayList<>()).add(ore);
        }

        for (Map.Entry<String, JsonElement> s : structures.entrySet()) {
            JsonObject o = Json.obj(s.getValue());
            if (o == null) continue;
            structureBiomeTags.put(s.getKey(), Json.str(o, "biomes"));
            JsonObject overrides = Json.obj(o, "spawn_overrides");
            if (overrides == null) continue;
            for (Map.Entry<String, JsonElement> cat : overrides.entrySet()) {
                JsonObject c = Json.obj(cat.getValue());
                readSpawnList(s.getKey(), true, cat.getKey(), Json.list(c, "spawns"));
            }
        }
    }

    /** Spawns of a biome: 26.x under attributes → natural_mob_spawns, older under "spawners". */
    private static JsonObject spawnTable(JsonObject biome) {
        JsonObject attr = Json.obj(Json.obj(biome, "attributes"), "minecraft:gameplay/natural_mob_spawns");
        if (attr != null) {
            JsonObject arg = Json.obj(attr, "argument");
            JsonObject byCat = Json.obj(arg != null ? arg : attr, "spawns_by_category");
            if (byCat != null) return byCat;
        }
        return Json.obj(biome, "spawners");
    }

    private void readSpawns(String place, boolean structure, JsonObject byCategory) {
        if (byCategory == null) return;
        for (Map.Entry<String, JsonElement> cat : byCategory.entrySet()) {
            List<JsonElement> list = new ArrayList<>();
            if (cat.getValue().isJsonArray()) cat.getValue().getAsJsonArray().forEach(list::add);
            readSpawnList(place, structure, cat.getKey(), list);
        }
    }

    private void readSpawnList(String place, boolean structure, String category, List<JsonElement> list) {
        for (JsonElement el : list) {
            JsonObject sp = Json.obj(el);
            if (sp == null) continue;
            String type = Json.id(Json.str(sp, "type"));
            if (type.isEmpty()) continue;
            Json.Range count = sp.has("count") ? Json.range(sp.get("count"), Json.Range.ONE)
                    : new Json.Range(Json.range(sp.get("minCount"), Json.Range.ONE).min(), Json.range(sp.get("maxCount"), Json.Range.ONE).max(), 1);
            int weight = (int) Json.range(sp.get("weight"), Json.Range.ONE).avg();
            spawns.computeIfAbsent(type, k -> new ArrayList<>()).add(new Spawn(place, structure, category, weight, count.min(), count.max()));
        }
    }

    /** Blocks that an ore feature places (ore, scattered_ore; 26.x directly, older under "config"). */
    private static List<String> oreTargets(JsonObject feature) {
        if (feature == null) return List.of();
        String type = Json.id(Json.str(feature, "type"));
        if (!type.equals("minecraft:ore") && !type.equals("minecraft:scattered_ore")) return List.of();
        JsonObject config = Json.obj(feature, "config");
        if (config == null) config = feature;
        List<String> out = new ArrayList<>();
        for (JsonElement t : Json.list(config, "targets")) {
            JsonObject target = Json.obj(t);
            if (target == null) continue;
            JsonElement state = target.get("state");
            String block = state == null ? "" : state.isJsonPrimitive() ? state.getAsString() : Json.str(Json.obj(state), "Name");
            if (!block.isEmpty() && !out.contains(Json.id(block))) out.add(Json.id(block));
        }
        return out;
    }

    private static int anchor(JsonObject a, Dimension dim, int fallback) {
        if (a == null) return fallback;
        if (a.has("absolute")) return a.get("absolute").getAsInt();
        if (a.has("above_bottom")) return dim.minY + a.get("above_bottom").getAsInt();
        if (a.has("below_top")) return dim.maxY - a.get("below_top").getAsInt();
        return fallback;
    }

    private static Set<String> tagMembers(Map<String, JsonElement> tags, String tag, int depth) {
        Set<String> out = new HashSet<>();
        if (depth > 6) return out;
        for (JsonElement v : Json.list(Json.obj(tags.get(tag)), "values")) {
            String ref = v.isJsonPrimitive() ? v.getAsString() : Json.str(Json.obj(v), "id");
            if (ref.startsWith("#")) out.addAll(tagMembers(tags, Json.id(ref.substring(1)), depth + 1));
            else if (!ref.isEmpty()) out.add(Json.id(ref));
        }
        return out;
    }

    public List<Spawn> spawnsOf(String entity) { return spawns.getOrDefault(entity, List.of()); }

    public List<Ore> oresOf(String block) { return ores.getOrDefault(block, List.of()); }

    public Set<String> structures() { return structureBiomeTags.keySet(); }
}
