package dev.aio.core.modules.inventory.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Trades from the data packs (data-driven since 26.x): {@code trade_set/*} picks from a list
 * or a tag ({@code tags/villager_trade/*}) single trades ({@code villager_trade/*}).
 */
public final class TradeData {
    /** An item with count (range). */
    public record Stack(String item, double min, double max) {
        public boolean isEmpty() { return item.isEmpty(); }

        /** Fixed count for the icon; 0 = unknown/random. */
        public int iconCount() { return min == max ? (int) min : 0; }

        public String countText() { return new Json.Range(min, max, (min + max) / 2).text(); }
    }

    public static final Stack NONE = new Stack("", 0, 0);

    public record Trade(String id, Stack wants, Stack additionalWants, Stack gives, int maxUses, int xp, boolean randomized) {}

    /** A trade set: how many offers are drawn and from which ones. */
    public record TradeSet(String id, int amount, List<Trade> trades) {}

    private final Map<String, JsonElement> sets, trades, tags;

    public TradeData(Map<String, JsonElement> tradeSets, Map<String, JsonElement> villagerTrades, Map<String, JsonElement> tradeTags) {
        this.sets = tradeSets;
        this.trades = villagerTrades;
        this.tags = tradeTags;
    }

    public Set<String> setIds() { return sets.keySet(); }

    public TradeSet set(String id) {
        JsonObject o = Json.obj(sets.get(Json.id(id)));
        if (o == null) return new TradeSet(id, 0, List.of());
        int amount = (int) Math.round(Json.range(o.get("amount"), Json.Range.ONE).avg());
        Set<String> ids = new LinkedHashSet<>();
        for (JsonElement e : Json.list(o, "trades")) resolve(e, ids, 0);
        List<Trade> out = new ArrayList<>();
        for (String t : ids) {
            Trade tr = trade(t);
            if (tr != null) out.add(tr);
        }
        return new TradeSet(id, amount, out);
    }

    /** Resolve entries: id, "#tag" (also nested) or {"id": ..., "required": ...}. */
    private void resolve(JsonElement e, Set<String> out, int depth) {
        if (depth > 8 || e == null) return;
        String ref;
        if (e.isJsonPrimitive()) ref = e.getAsString();
        else if (e.isJsonObject() && e.getAsJsonObject().has("id")) ref = Json.str(e.getAsJsonObject(), "id");
        else return;
        if (ref.startsWith("#")) {
            JsonObject tag = Json.obj(tags.get(Json.id(ref.substring(1))));
            for (JsonElement v : Json.list(tag, "values")) resolve(v, out, depth + 1);
        } else {
            out.add(Json.id(ref));
        }
    }

    Trade trade(String id) {
        JsonObject o = Json.obj(trades.get(id));
        if (o == null) return null;
        Stack wants = stack(Json.obj(o, "wants"));
        Stack gives = stack(Json.obj(o, "gives"));
        if (wants.isEmpty() || gives.isEmpty()) return null;
        boolean randomized = !Json.list(o, "given_item_modifier").isEmpty() || !Json.list(o, "given_item_modifiers").isEmpty();
        return new Trade(id, wants, stack(Json.obj(o, "additional_wants")), gives,
                (int) Json.range(o.get("max_uses"), new Json.Range(12, 12, 12)).avg(),
                (int) Json.range(o.get("xp"), new Json.Range(1, 1, 1)).avg(), randomized);
    }

    private static Stack stack(JsonObject o) {
        if (o == null) return NONE;
        String item = Json.id(Json.str(o, "id"));
        if (item.isEmpty()) item = Json.id(Json.str(o, "item"));
        if (item.isEmpty()) return NONE;
        Json.Range r = Json.range(o.get("count"), Json.Range.ONE);
        return new Stack(item, r.min(), r.max());
    }
}
