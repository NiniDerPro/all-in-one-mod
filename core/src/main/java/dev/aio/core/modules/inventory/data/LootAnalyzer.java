package dev.aio.core.modules.inventory.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Evaluates a loot table (JSON): which items drop with what probability and in
 * what quantity? Approximation as usual for loot displays: per pool weight/total weight per roll,
 * rolls averaged, random conditions multiplied, nested tables proportionally.
 */
public final class LootAnalyzer {
    /** One possible item. {@code chance} = probability for at least one (0..1). */
    public record Drop(String item, double chance, double min, double max, boolean conditional) {
        public String countText() {
            Json.Range r = new Json.Range(min, max, (min + max) / 2);
            return r.text();
        }
    }

    private final Map<String, JsonElement> tables;

    /** @param tables all loot tables by id ("minecraft:chests/desert_pyramid") */
    public LootAnalyzer(Map<String, JsonElement> tables) { this.tables = tables; }

    public List<Drop> analyze(String tableId) {
        Map<String, Acc> acc = new LinkedHashMap<>();
        collect(Json.obj(tables.get(tableId)), 1.0, acc, new HashSet<>(Set.of(tableId)), 0);
        return acc.entrySet().stream()
                .map(e -> new Drop(e.getKey(), Math.min(1, 1 - e.getValue().notDropped), e.getValue().min, e.getValue().max, e.getValue().conditional))
                .filter(d -> d.chance() > 0)
                .sorted((a, b) -> Double.compare(b.chance(), a.chance()))
                .toList();
    }

    private static final class Acc {
        double notDropped = 1;
        double min = Double.MAX_VALUE, max = 0;
        boolean conditional;
    }

    private void collect(JsonObject table, double scale, Map<String, Acc> acc, Set<String> seen, int depth) {
        if (table == null || depth > 6) return;
        for (JsonElement p : Json.list(table, "pools")) {
            JsonObject pool = Json.obj(p);
            if (pool == null) continue;
            double rolls = Json.range(pool.get("rolls"), Json.Range.ONE).avg();
            Cond poolCond = conditions(pool);
            List<Weighted> entries = new ArrayList<>();
            flatten(Json.list(pool, "entries"), entries, 0);
            double total = 0;
            for (Weighted w : entries) total += w.weight;
            if (total <= 0) continue;
            for (Weighted w : entries) {
                JsonObject e = w.entry;
                String type = Json.id(Json.str(e, "type"));
                Cond own = conditions(e);
                Cond c = own.and(poolCond);
                // Pool condition applies to the whole pool, entry condition per roll
                double perRoll = w.weight / total * own.chance;
                double p1 = poolCond.chance * (1 - Math.pow(1 - Math.min(1, perRoll), Math.max(rolls, 0))) * scale;
                if (type.equals("minecraft:item")) {
                    String item = Json.id(Json.str(e, "name"));
                    Json.Range count = count(e);
                    Acc a = acc.computeIfAbsent(item, k -> new Acc());
                    a.notDropped *= 1 - Math.min(1, p1);
                    a.min = Math.min(a.min, Math.max(0, count.min())); // negative Anzahl = nichts
                    a.max = Math.max(a.max, count.max());
                    a.conditional |= c.other;
                } else if (type.equals("minecraft:loot_table")) {
                    JsonElement v = e.get("value");
                    if (v != null && v.isJsonPrimitive()) {
                        String id = Json.id(v.getAsString());
                        if (seen.add(id)) collect(Json.obj(tables.get(id)), p1, acc, seen, depth + 1);
                    } else {
                        collect(Json.obj(v), p1, acc, seen, depth + 1);
                    }
                }
            }
        }
    }

    private record Weighted(JsonObject entry, double weight) {}

    /** Resolve composite entries (alternatives, group, sequence) into their children. */
    private static void flatten(List<JsonElement> list, List<Weighted> out, int depth) {
        for (JsonElement el : list) {
            JsonObject e = Json.obj(el);
            if (e == null) continue;
            String type = Json.id(Json.str(e, "type"));
            if (type.equals("minecraft:empty")) {
                out.add(new Weighted(e, e.has("weight") ? e.get("weight").getAsDouble() : 1));
                continue;
            }
            if (e.has("children") && depth < 5) {
                flatten(Json.list(e, "children"), out, depth + 1);
                continue;
            }
            double weight = e.has("weight") ? e.get("weight").getAsDouble() : 1;
            if (!type.isEmpty()) out.add(new Weighted(e, weight));
        }
    }

    /** Functions/modifiers: 26.3 "modifier" (type in "type"), older "functions" (type in "function"). */
    private static List<JsonElement> modifiers(JsonObject o) {
        List<JsonElement> out = new ArrayList<>(Json.list(o, "modifier"));
        out.addAll(Json.list(o, "functions"));
        return out;
    }

    private static String typeOf(JsonObject o, String legacyKey) {
        String t = Json.str(o, "type");
        return Json.id(t.isEmpty() ? Json.str(o, legacyKey) : t);
    }

    /** Count from set_count (without "add"). */
    private static Json.Range count(JsonObject entry) {
        Json.Range r = Json.Range.ONE;
        for (JsonElement f : modifiers(entry)) {
            JsonObject fn = Json.obj(f);
            if (fn == null) continue;
            if (typeOf(fn, "function").equals("minecraft:set_count") && !(fn.has("add") && fn.get("add").getAsBoolean())) {
                r = Json.range(fn.get("count"), r);
            }
        }
        return r;
    }

    /** Random chance of the conditions; {@code other} = there are further conditions (e.g. "killed by player"). */
    private record Cond(double chance, boolean other) {
        Cond and(Cond o) { return new Cond(chance * o.chance, other || o.other); }
    }

    private static final Cond ALWAYS = new Cond(1, false);

    /** Conditions: 26.3 "condition" (single or list), older "conditions". */
    private static Cond conditions(JsonObject o) {
        Cond c = ALWAYS;
        for (JsonElement el : Json.list(o, "condition")) c = c.and(condition(el, 0));
        for (JsonElement el : Json.list(o, "conditions")) c = c.and(condition(el, 0));
        return c;
    }

    private static Cond condition(JsonElement el, int depth) {
        JsonObject c = Json.obj(el);
        if (c == null || depth > 5) return new Cond(1, true); // reference to a predicate: unknown
        String type = typeOf(c, "condition");
        switch (type) {
            case "minecraft:random_chance":
                return new Cond(Json.range(c.get("chance"), Json.Range.ONE).avg(), false);
            case "minecraft:random_chance_with_enchanted_bonus":
            case "minecraft:random_chance_with_looting":
                return new Cond(Json.range(c.has("unenchanted_chance") ? c.get("unenchanted_chance") : c.get("chance"), Json.Range.ONE).avg(), false);
            case "minecraft:survives_explosion":
                return ALWAYS; // always fulfilled when nothing explodes
            case "minecraft:all_of": {
                Cond r = ALWAYS;
                for (JsonElement t : Json.list(c, "terms")) r = r.and(condition(t, depth + 1));
                return r;
            }
            default:
                return new Cond(1, true);
        }
    }
}
