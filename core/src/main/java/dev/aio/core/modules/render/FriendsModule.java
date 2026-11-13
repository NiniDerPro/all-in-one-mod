package dev.aio.core.modules.render;

import dev.aio.core.i18n.Lang;
import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.setting.ActionSetting;
import dev.aio.core.setting.EnumSetting;
import dev.aio.core.setting.NumberSetting;
import dev.aio.core.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Friends: rules by player name (exact, contains, starts or ends with) and a colour per rule.
 * Matching players are marked in that colour, either as an outline or by tinting the whole skin.
 * Contains rules also check the name the server shows (team prefix, tab list), so tags work too.
 */
public final class FriendsModule extends Module {
    /** Colour value for "no rule matches". Stored colours always carry full alpha, so they are never 0. */
    public static final int NONE = 0;
    public static final int FRIEND_GREEN = 0xFF55FF55;
    public static final int TAG_COLOR = 0xFF55FFFF;
    public static final int[] PALETTE = {0xFF55FF55, 0xFF00AA00, 0xFF55FFFF, 0xFF5599FF, 0xFFAA55FF, 0xFFFF55FF,
            0xFFFF5555, 0xFFFFAA00, 0xFFFFFF55, 0xFFFFFFFF};

    public enum Match {
        NAME, CONTAINS, STARTS, ENDS;

        public String label() {
            return switch (this) {
                case NAME -> Lang.tr("aio.ui.friends.match_name", "Name is");
                case CONTAINS -> Lang.tr("aio.ui.friends.match_contains", "Name contains");
                case STARTS -> Lang.tr("aio.ui.friends.match_starts", "Starts with");
                case ENDS -> Lang.tr("aio.ui.friends.match_ends", "Ends with");
            };
        }

        public Match next() { return values()[(ordinal() + 1) % values().length]; }

        /** Default colour for new rules: green for friends by name, otherwise a second colour. */
        public int defaultColor() { return this == NAME ? FRIEND_GREEN : TAG_COLOR; }
    }

    public enum Marking implements EnumSetting.Named {
        OUTLINE("Outline"), FULL("Full"), BOTH("Outline and fill");

        private final String displayName;

        Marking(String displayName) { this.displayName = displayName; }

        @Override public String displayName() { return displayName; }
    }

    /** One rule of the list. */
    public record Rule(Match match, String text, int color, boolean enabled) {
        public Rule {
            text = text == null ? "" : text.trim();
            color |= 0xFF000000;
        }

        public Rule withMatch(Match m) { return new Rule(m, text, color, enabled); }

        public Rule withText(String t) { return new Rule(match, t, color, enabled); }

        public Rule withColor(int c) { return new Rule(match, text, c, enabled); }

        public Rule withEnabled(boolean e) { return new Rule(match, text, color, e); }

        boolean matches(String account, String display, String tab) {
            if (!enabled || text.isEmpty()) return false;
            String t = text.toLowerCase(Locale.ROOT);
            if (match == Match.NAME) return t.equals(account) || t.equals(display) || t.equals(tab);
            return test(t, account) || test(t, display) || test(t, tab);
        }

        private boolean test(String t, String name) {
            if (name.isEmpty()) return false;
            return switch (match) {
                case CONTAINS -> name.contains(t);
                case STARTS -> name.startsWith(t);
                case ENDS -> name.endsWith(t);
                case NAME -> name.equals(t);
            };
        }
    }

    static final String FILE = "friends.json";
    public static final int MAX_TEXT = 32;

    private final EnumSetting<Marking> style;
    private final NumberSetting strength;
    private final List<Rule> rules = new ArrayList<>();
    private final Map<String, Integer> cache = new HashMap<>();
    private boolean loaded;

    public FriendsModule() {
        super("friends", "Friends", "Marks friends and players with chosen names or tags in a colour", Category.VISUALS, false);
        add(new ActionSetting("manage", "Edit friends and rules", () -> core.platform().openScreen(new FriendsScreen(core, this))));
        style = add(new EnumSetting<>("style", "Marking", Marking.FULL));
        strength = add(new NumberSetting("strength", "Colour strength", 60, 20, 100, 5, " %"));
        strength.visibleWhen(() -> style.get() != Marking.OUTLINE);
    }

    @Override protected void onActivate() { load(); }

    public Marking style() { return style.get(); }

    /** Outline in the rule colour (Outline, Outline and fill). */
    public boolean outline() { return style.get() != Marking.FULL; }

    /** Tint the skin and armour in the rule colour (Full, Outline and fill). */
    public boolean fill() { return style.get() != Marking.OUTLINE; }

    /**
     * Colour for a player or {@link #NONE}. The names may contain formatting codes; they are compared
     * without them and without case. The first matching rule in the list wins.
     *
     * @param account login name
     * @param display name as shown in the world (with team prefix/suffix), may be null
     * @param tab     name in the tab list, may be null
     */
    public int colorFor(String account, String display, String tab) {
        if (!isActive()) return NONE;
        if (!loaded) load();
        if (rules.isEmpty()) return NONE;
        String a = clean(account), d = clean(display), t = clean(tab);
        String key = a + '\n' + d + '\n' + t;
        Integer hit = cache.get(key);
        if (hit != null) return hit;
        int color = NONE;
        for (Rule r : rules) {
            if (r.matches(a, d, t)) {
                color = r.color();
                break;
            }
        }
        if (cache.size() > 512) cache.clear();
        cache.put(key, color);
        return color;
    }

    /**
     * Multiplier for tinting the whole model (ARGB, white = unchanged): the rule colour mixed with
     * white according to the colour strength, so the skin stays recognisable.
     */
    public int tintFor(int color) {
        float s = strength.get().floatValue() / 100f;
        int r = mix(color >> 16), g = mix(color >> 8), b = mix(color);
        return 0xFF000000 | (lerp(r, s) << 16) | (lerp(g, s) << 8) | lerp(b, s);
    }

    private static int mix(int channel) { return channel & 0xFF; }

    private static int lerp(int c, float s) { return Math.round(255 + (c - 255) * s); }

    static String clean(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.replaceAll("§.", "").trim().toLowerCase(Locale.ROOT);
    }

    // ---- Daten ----

    public List<Rule> rules() {
        if (!loaded && core != null) load();
        return List.copyOf(rules);
    }

    public void addRule(Rule r) {
        if (r.text().isEmpty()) return;
        if (!loaded) load();
        for (Rule o : rules) {
            if (o.match() == r.match() && o.text().equalsIgnoreCase(r.text())) {
                core.platform().notify(Lang.tr("aio.ui.friends.exists", "\"%s\" is already in the list", r.text()));
                return;
            }
        }
        rules.add(r);
        changed();
    }

    public void replace(Rule old, Rule now) {
        int i = rules.indexOf(old);
        if (i < 0) return;
        if (now.text().isEmpty()) rules.remove(i);
        else rules.set(i, now);
        changed();
    }

    public void remove(Rule r) {
        if (rules.remove(r)) changed();
    }

    private void changed() {
        cache.clear();
        save();
    }

    private void load() {
        if (core == null) return;
        rules.clear();
        cache.clear();
        String json = core.config().readText(FILE);
        if (json != null) parseInto(json, rules);
        loaded = true;
    }

    static void parseInto(String json, List<Rule> out) {
        try {
            JsonElement el = JsonParser.parseString(json);
            if (!el.isJsonArray()) return;
            for (JsonElement e : el.getAsJsonArray()) {
                try {
                    JsonObject o = e.getAsJsonObject();
                    Match m = Match.valueOf(o.get("match").getAsString().toUpperCase(Locale.ROOT));
                    String text = o.get("text").getAsString();
                    if (text.isBlank()) continue;
                    int color = o.has("color") ? (int) Long.parseLong(o.get("color").getAsString().replace("#", ""), 16) : m.defaultColor();
                    boolean on = !o.has("enabled") || o.get("enabled").getAsBoolean();
                    out.add(new Rule(m, text.length() > MAX_TEXT ? text.substring(0, MAX_TEXT) : text, color, on));
                } catch (RuntimeException ignored) {
                    // skip a single broken entry, keep the rest
                }
            }
        } catch (RuntimeException e) {
            Log.warn("Could not read friends: " + e.getMessage());
        }
    }

    static String toJson(List<Rule> list) {
        JsonArray arr = new JsonArray();
        for (Rule r : list) {
            JsonObject o = new JsonObject();
            o.addProperty("match", r.match().name().toLowerCase(Locale.ROOT));
            o.addProperty("text", r.text());
            o.addProperty("color", String.format("#%06X", r.color() & 0xFFFFFF));
            o.addProperty("enabled", r.enabled());
            arr.add(o);
        }
        return arr.toString();
    }

    private void save() {
        core.config().writeText(FILE, toJson(rules));
    }
}
