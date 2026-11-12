package dev.aio.core.modules.inventory.data;

import dev.aio.core.platform.ItemRef;
import dev.aio.core.platform.Platform;
import dev.aio.core.platform.Profession;
import dev.aio.core.platform.RecipeView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds the additional pages of the item browser: loot/drops, trades, professions, piglin bartering,
 * ore deposits and spawns. Where something comes from = "Recipes" (R), what it is needed for = "Uses" (U).
 */
public final class InfoViews {
    private static final int ROWS_PER_PAGE = 7;
    private static final String[] LEVELS = {"", "Novice", "Apprentice", "Journeyman", "Expert", "Master"};
    private static final String VILLAGER_EGG = "minecraft:villager_spawn_egg";
    private static final String WANDERING_EGG = "minecraft:wandering_trader_spawn_egg";

    /** Display names for vanilla structures and chests (Minecraft does not translate them). */
    private static final Map<String, String> PLACES = new LinkedHashMap<>();

    static {
        String[][] names = {
                {"desert_pyramid", "Desert Pyramid"}, {"jungle_temple", "Jungle Temple"}, {"jungle_pyramid", "Jungle Temple"},
                {"ancient_city", "Ancient City"}, {"bastion", "Bastion Remnant"}, {"end_city", "End City"},
                {"nether_bridge", "Nether Fortress"}, {"fortress", "Nether Fortress"}, {"stronghold", "Stronghold"},
                {"woodland_mansion", "Woodland Mansion"}, {"mansion", "Woodland Mansion"}, {"shipwreck", "Shipwreck"},
                {"buried_treasure", "Buried Treasure"}, {"igloo", "Igloo"}, {"pillager_outpost", "Pillager Outpost"},
                {"ruined_portal", "Ruined Portal"}, {"simple_dungeon", "Dungeon"}, {"abandoned_mineshaft", "Mineshaft"},
                {"mineshaft", "Mineshaft"}, {"underwater_ruin", "Ocean Ruin"}, {"ocean_ruin", "Ocean Ruin"},
                {"monument", "Ocean Monument"}, {"swamp_hut", "Swamp Hut"}, {"trial_chambers", "Trial Chambers"},
                {"trial_chamber", "Trial Chambers"}, {"spawn_bonus_chest", "Bonus Chest"}, {"abandoned_camp", "Abandoned Camp"},
                {"village", "Village"}, {"nether_fossil", "Nether Fossil"}, {"trail_ruins", "Trail Ruins"}};
        for (String[] n : names) PLACES.put(n[0], n[1]);
    }

    private final Platform p;
    private final WorldInfo info;

    public InfoViews(Platform p, WorldInfo info) {
        this.p = p;
        this.info = info;
    }

    /** Woher kommt das Item? */
    public List<RecipeView> sources(ItemRef item) {
        List<RecipeView> out = new ArrayList<>();
        String id = item.id();
        // Beute und Drops
        List<RecipeView.Row> rows = new ArrayList<>();
        for (WorldInfo.Source s : info.sourcesOf(id)) {
            rows.add(new RecipeView.Row(sourceIcon(s.table()), sourceName(s.table()) + " · " + chance(s.drop()) + countSuffix(s.drop())));
        }
        page(out, "Loot & Drops", p.item("minecraft:chest", 1), List.of(), rows);
        // Trade that gives the item
        for (WorldInfo.TradeEntry t : info.trades()) if (t.trade().gives().item().equals(id)) out.add(tradeView(t));
        // Erzvorkommen
        List<RecipeView.Row> ores = new ArrayList<>();
        List<String> oreNotes = new ArrayList<>();
        for (WorldGenData.Ore ore : info.worldGen().oresOf(id)) {
            ores.add(new RecipeView.Row(item, "Y " + ore.minY() + " to " + ore.maxY() + " · ~" + num(ore.perChunk()) + "× per chunk"
                    + (ore.shape().equals("trapezoid") ? " · mostly in the middle" : "")));
            if (oreNotes.isEmpty()) oreNotes.add(biomeSummary(ore));
        }
        page(out, "Generation", item, oreNotes, ores);
        // Spawns (Spawn-Ei → Biome und Strukturen)
        String entity = entityOfEgg(id);
        if (entity != null) {
            List<RecipeView.Row> spawnRows = new ArrayList<>();
            List<WorldGenData.Spawn> spawns = new ArrayList<>(info.worldGen().spawnsOf(entity));
            spawns.sort((a, b) -> Integer.compare(b.weight(), a.weight()));
            for (WorldGenData.Spawn s : spawns) {
                String place = s.structure() ? placeName(s.place()) : p.translate("biome." + s.place().replace(':', '.'), Json.pretty(s.place()));
                String count = s.minCount() == s.maxCount() ? num(s.minCount()) : num(s.minCount()) + "–" + num(s.maxCount());
                spawnRows.add(new RecipeView.Row(p.item(s.structure() ? "minecraft:mossy_stone_bricks" : "minecraft:grass_block", 1),
                        place + " · weight " + s.weight() + " · group " + count));
            }
            page(out, "Spawns in", item, List.of(), spawnRows);
        }
        if (id.equals(VILLAGER_EGG)) out.addAll(professionList());
        return out;
    }

    /** What is the item needed for? */
    public List<RecipeView> usages(ItemRef item) {
        List<RecipeView> out = new ArrayList<>();
        String id = item.id();
        // Workstation block → profession (like JEP) and its trades
        for (Profession prof : info.professions()) {
            if (!prof.jobBlock().equals(id)) continue;
            out.add(RecipeView.withNotes("Profession", List.of(List.of(item)), p.item(VILLAGER_EGG, 1), item,
                    List.of("Profession: " + prof.name(), "A villager nearby takes this profession.")));
            for (WorldInfo.TradeEntry t : info.trades()) if (t.profession() == prof) out.add(tradeView(t));
        }
        if (id.equals(VILLAGER_EGG) || id.equals("minecraft:emerald")) out.addAll(professionList());
        if (id.equals(WANDERING_EGG)) {
            for (WorldInfo.TradeEntry t : info.trades()) if (t.profession() == null) out.add(tradeView(t));
        }
        // Trade that asks for the item
        for (WorldInfo.TradeEntry t : info.trades()) {
            TradeData.Trade tr = t.trade();
            if (tr.wants().item().equals(id) || tr.additionalWants().item().equals(id)) out.add(tradeView(t));
        }
        // Piglin-Tausch mit Gold
        if (id.equals("minecraft:gold_ingot")) {
            page(out, "Piglin Bartering", p.item("minecraft:piglin_head", 1), List.of("One gold ingot gives one of these:"),
                    dropRows(info.drops(WorldInfo.BARTERING)));
        }
        // Spawn egg → what the mob drops
        String entity = entityOfEgg(id);
        if (entity != null) {
            String table = entity.replace(":", ":entities/");
            page(out, "Drops", item, List.of(), dropRows(info.drops(table)));
        }
        return out;
    }

    private List<RecipeView> professionList() {
        List<RecipeView.Row> rows = new ArrayList<>();
        for (Profession prof : info.professions()) {
            if (prof.jobBlock().isEmpty()) continue;
            ItemRef block = p.item(prof.jobBlock(), 1);
            String ns = prof.id().contains(":") ? prof.id().substring(0, prof.id().indexOf(':')) : "minecraft";
            String mod = ns.equals("minecraft") ? "" : " [" + ns + "]"; // professions from mods with mod id
            rows.add(new RecipeView.Row(block, prof.name() + mod + " · " + block.name()));
        }
        List<RecipeView> out = new ArrayList<>();
        page(out, "Professions", p.item(VILLAGER_EGG, 1), List.of("Job block → profession"), rows);
        return out;
    }

    private List<RecipeView.Row> dropRows(List<LootAnalyzer.Drop> drops) {
        List<RecipeView.Row> rows = new ArrayList<>();
        for (LootAnalyzer.Drop d : drops) {
            ItemRef it = p.item(d.item(), 1);
            if (it.isEmpty()) continue;
            rows.add(new RecipeView.Row(it, it.name() + " · " + chance(d) + countSuffix(d)));
        }
        return rows;
    }

    private RecipeView tradeView(WorldInfo.TradeEntry t) {
        TradeData.Trade tr = t.trade();
        List<List<ItemRef>> slots = new ArrayList<>();
        slots.add(List.of(stack(tr.wants())));
        if (!tr.additionalWants().isEmpty()) slots.add(List.of(stack(tr.additionalWants())));
        List<String> notes = new ArrayList<>();
        ItemRef station;
        if (t.profession() == null) {
            notes.add("Wandering Trader · " + t.rarity());
            station = p.item(WANDERING_EGG, 1);
        } else {
            notes.add(t.profession().name() + " · level " + t.level() + (t.level() < LEVELS.length ? " (" + LEVELS[t.level()] + ")" : ""));
            station = p.item(t.profession().jobBlock(), 1);
        }
        String price = !tr.wants().isEmpty() && tr.wants().iconCount() == 0 ? "Price: " + priceText(tr.wants()) + " · " : "";
        notes.add(price + "max " + tr.maxUses() + "× · " + tr.xp() + " XP" + (tr.randomized() ? " · random enchantment" : ""));
        return RecipeView.withNotes("Trade", slots, stack(tr.gives()), station, notes);
    }

    private String priceText(TradeData.Stack s) {
        return s.min() <= 0 ? "depends on enchantment" : s.countText() + " " + p.item(s.item(), 1).name();
    }

    private ItemRef stack(TradeData.Stack s) {
        int count = s.iconCount() > 0 ? s.iconCount() : 1;
        return p.item(s.item(), count);
    }

    /** Split a list into pages of {@link #ROWS_PER_PAGE} rows each. */
    private static void page(List<RecipeView> out, String type, ItemRef icon, List<String> notes, List<RecipeView.Row> rows) {
        for (int i = 0; i < rows.size(); i += ROWS_PER_PAGE) {
            out.add(RecipeView.list(type, icon, notes, rows.subList(i, Math.min(rows.size(), i + ROWS_PER_PAGE))));
        }
    }

    private ItemRef sourceIcon(String table) {
        String path = table.substring(table.indexOf(':') + 1);
        String icon;
        if (path.startsWith("entities/")) {
            String egg = table.substring(0, table.indexOf(':') + 1) + Json.lastPath(path) + "_spawn_egg";
            ItemRef e = p.item(egg, 1);
            if (!e.isEmpty()) return e;
            icon = "minecraft:bone";
        } else if (path.equals("gameplay/piglin_bartering")) icon = "minecraft:gold_ingot";
        else if (path.startsWith("gameplay/fishing")) icon = "minecraft:fishing_rod";
        else if (path.startsWith("archaeology/")) icon = "minecraft:brush";
        else if (path.startsWith("shearing/")) icon = "minecraft:shears";
        else if (path.startsWith("spawners/")) icon = "minecraft:trial_spawner";
        else if (path.startsWith("pots/")) icon = "minecraft:decorated_pot";
        else if (path.startsWith("dispensers/")) icon = "minecraft:dispenser";
        else if (path.startsWith("equipment/")) icon = "minecraft:iron_chestplate";
        else if (path.startsWith("gameplay/")) icon = "minecraft:emerald";
        else icon = "minecraft:chest";
        ItemRef r = p.item(icon, 1);
        return r.isEmpty() ? p.item("minecraft:chest", 1) : r;
    }

    String sourceName(String table) {
        String ns = table.substring(0, table.indexOf(':'));
        String path = table.substring(table.indexOf(':') + 1);
        if (path.startsWith("entities/")) {
            String e = Json.lastPath(path);
            return p.translate("entity." + ns + "." + e, Json.pretty(e));
        }
        if (path.equals("gameplay/piglin_bartering")) return "Piglin Bartering";
        if (path.startsWith("gameplay/fishing")) return "Fishing" + (path.endsWith("treasure") ? " (treasure)" : path.endsWith("junk") ? " (junk)" : "");
        if (path.startsWith("gameplay/hero_of_the_village")) return "Hero of the Village";
        if (path.startsWith("archaeology/")) return "Archaeology: " + placeName(Json.lastPath(path));
        if (path.startsWith("shearing/")) return "Shearing: " + Json.pretty(path.substring("shearing/".length()).replace('/', '_'));
        if (path.startsWith("chests/village/")) return "Village: " + Json.pretty(Json.lastPath(path).replace("village_", ""));
        if (path.startsWith("chests/")) return placeName(path.substring("chests/".length()));
        if (path.startsWith("spawners/")) return "Trial Spawner";
        return Json.pretty(path);
    }

    /** Structure/chest name: known vanilla names as display names, otherwise from the id. */
    String placeName(String id) {
        String path = Json.lastPath(id.replace('/', '_'));
        for (Map.Entry<String, String> e : PLACES.entrySet()) {
            if (path.startsWith(e.getKey())) {
                String rest = path.substring(e.getKey().length()).replace('_', ' ').trim();
                return rest.isEmpty() ? e.getValue() : e.getValue() + " (" + rest + ")";
            }
        }
        return Json.pretty(path);
    }

    private String biomeSummary(WorldGenData.Ore ore) {
        int n = ore.biomes().size();
        String dim = switch (ore.dimension()) { case NETHER -> "Nether"; case END -> "End"; default -> "Overworld"; };
        if (n > 12) return dim + " · in " + n + " biomes";
        List<String> names = new ArrayList<>();
        for (String b : ore.biomes()) names.add(p.translate("biome." + b.replace(':', '.'), Json.pretty(b)));
        return dim + ": " + String.join(", ", names);
    }

    /** "minecraft:zombie_spawn_egg" → "minecraft:zombie". */
    private static String entityOfEgg(String itemId) {
        return itemId.endsWith("_spawn_egg") ? itemId.substring(0, itemId.length() - "_spawn_egg".length()) : null;
    }

    static String chance(LootAnalyzer.Drop d) {
        double pct = d.chance() * 100;
        String s = pct >= 10 ? String.format(Locale.GERMANY, "%.0f %%", pct)
                : pct >= 1 ? String.format(Locale.GERMANY, "%.1f %%", pct) : String.format(Locale.GERMANY, "%.2f %%", pct);
        return (d.conditional() ? "up to " : "") + s;
    }

    private static String countSuffix(LootAnalyzer.Drop d) {
        return d.min() == 1 && d.max() == 1 ? "" : " · " + d.countText() + "×";
    }

    private static String num(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format(Locale.GERMANY, "%.1f", v);
    }
}
