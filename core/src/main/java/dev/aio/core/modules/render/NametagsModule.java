package dev.aio.core.modules.render;

import dev.aio.core.hud.HudElement;
import dev.aio.core.i18n.Lang;
import dev.aio.core.module.Category;
import dev.aio.core.modules.navigation.Projection;
import dev.aio.core.platform.CameraView;
import dev.aio.core.platform.ItemRef;
import dev.aio.core.platform.NametagTarget;
import dev.aio.core.platform.Renderer;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.ColorSetting;
import dev.aio.core.setting.EnumSetting;
import dev.aio.core.setting.NumberSetting;
import dev.aio.core.util.Colors;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Nametags: own nametags above players (health, ping, game mode, distance, equipment
 * with enchantments and durability), items on the ground (name and count), mobs (health) and TNT
 * (fuse time). Visible through walls too, replaces the vanilla tags of the chosen objects.
 */
public final class NametagsModule extends HudElement {
    public enum Durability implements EnumSetting.Named {
        BAR("Bar"), PERCENT("Percent"), NUMBER("Number"), OFF("Off");

        private final String displayName;

        Durability(String displayName) { this.displayName = displayName; }

        @Override public String displayName() { return displayName; }
    }

    private static final int ICON = 16;

    /**
     * Size by distance: up to 4 blocks full, then smoothly smaller (8 → 66 %, from about 12 → 50 %).
     * The Minecraft font becomes unreadable when smaller; {@link #declutter} prevents overlapping.
     */
    static float distanceScale(double distance) {
        if (distance <= 4) return 1f;
        return (float) Math.max(0.5, Math.pow(4.0 / distance, 0.6));
    }

    /** One tag on the screen: target, anchor point, size and rough area of the name row. */
    record Placed(NametagTarget target, float x, float y, float scale, float x1, float y1, float x2, float y2) {
        float area() { return (x2 - x1) * (y2 - y1); }

        float overlap(Placed o) {
            float w = Math.min(x2, o.x2) - Math.max(x1, o.x1), h = Math.min(y2, o.y2) - Math.max(y1, o.y1);
            return w > 0 && h > 0 ? w * h : 0;
        }
    }

    /**
     * From near to far: a tag that would lie more than 40 % behind a nearer one is dropped.
     * That way in groups far away it stays readable who stands in front, instead of a tangle of tags.
     */
    static List<Placed> declutter(List<Placed> nearToFar) {
        List<Placed> kept = new ArrayList<>();
        outer:
        for (Placed p : nearToFar) {
            for (Placed k : kept) if (p.overlap(k) > 0.4f * p.area()) continue outer;
            kept.add(p);
        }
        return kept;
    }

    private final BooleanSetting players, items, mobs, tnt, throughWalls;
    private final BooleanSetting health, ping, gameMode, distance;
    private final BooleanSetting gear, enchants, skipEmpty, itemCount;
    private final EnumSetting<Durability> durability;
    private final NumberSetting range, maxCount, size;
    private final ColorSetting background;

    public NametagsModule() {
        super("nametags", "Nametags", "Better nametags: health, ping and gear on players, names on items, TNT fuse", Category.VISUALS, false, 0, 0);
        hideBoxSettings();
        players = add(new BooleanSetting("players", "Players", true));
        items = add(new BooleanSetting("items", "Items on the ground", true));
        mobs = add(new BooleanSetting("mobs", "Mobs", false));
        tnt = add(new BooleanSetting("tnt", "TNT fuse", true));
        throughWalls = add(new BooleanSetting("walls", "Show through walls", true));
        health = add(new BooleanSetting("health", "Health", true));
        ping = add(new BooleanSetting("ping", "Ping", true));
        gameMode = add(new BooleanSetting("gameMode", "Game mode", false));
        distance = add(new BooleanSetting("distance", "Distance", false));
        gear = add(new BooleanSetting("gear", "Armor and held items", true));
        enchants = add(new BooleanSetting("enchants", "Enchantments", true));
        enchants.visibleWhen(gear::on);
        durability = add(new EnumSetting<>("durability", "Durability", Durability.BAR));
        durability.visibleWhen(gear::on);
        skipEmpty = add(new BooleanSetting("skipEmpty", "Skip empty slots", true));
        skipEmpty.visibleWhen(gear::on);
        itemCount = add(new BooleanSetting("itemCount", "Item count", true));
        range = add(new NumberSetting("range", "Max distance", 64, 8, 256, 8, " blocks"));
        maxCount = add(new NumberSetting("maxCount", "Max nametags", 50, 5, 200, 5, ""));
        size = add(new NumberSetting("size", "Size", 100, 50, 200, 10, " %"));
        background = add(new ColorSetting("tagBackground", "Nametag background", 0x90101014));
    }

    @Override public int width(Renderer r) { return 1; }

    @Override public int height(Renderer r) { return 1; }

    @Override public boolean editable() { return false; }

    @Override protected boolean drawsDefaultBackground() { return false; }

    @Override protected void renderContent(Renderer r, boolean preview) {}

    @Override public boolean hasContent() { return platform().inWorld() && !platform().isScreenOpen(); }

    /** Whether the vanilla nametags of this kind are hidden (this module shows its own). */
    public boolean replaces(NametagTarget.Kind kind) {
        if (!isActive()) return false;
        return switch (kind) {
            case PLAYER -> players.on();
            case MOB -> mobs.on();
            case ITEM -> items.on();
            case TNT -> tnt.on();
        };
    }

    @Override public void renderOverlay(Renderer r, float partialTick) {
        if (!isActive() || !hasContent()) return;
        List<NametagTarget> list = new ArrayList<>(platform().nametagTargets(range.get(), partialTick,
                players.on(), mobs.on(), items.on(), tnt.on(), throughWalls.on()));
        if (list.isEmpty()) return;
        // select the nearest first, then draw from back to front
        list.sort((a, b) -> Double.compare(a.distance(), b.distance()));
        if (list.size() > maxCount.intValue()) list = list.subList(0, maxCount.intValue());
        CameraView cam = platform().camera(partialTick);
        float w = r.screenWidth(), h = r.screenHeight();
        float base = (float) (size.get() / 100.0);
        int fh = r.fontHeight();
        List<Placed> placed = new ArrayList<>(list.size());
        for (NametagTarget t : list) {
            Projection.Point pt = Projection.project(t.x(), t.y(), t.z(), cam, w, h);
            if (!pt.onScreen()) continue;
            float scale = base * distanceScale(t.distance());
            float half = (r.textWidth(t.name()) / 2f + 18) * scale; // Name plus Herz/Ping grob
            placed.add(new Placed(t, pt.x(), pt.y(), scale, pt.x() - half, pt.y() - (fh + 3) * scale, pt.x() + half, pt.y()));
        }
        List<Placed> kept = declutter(placed);
        for (int i = kept.size() - 1; i >= 0; i--) { // draw from back to front
            Placed p = kept.get(i);
            NametagTarget t = p.target();
            r.push();
            r.translate(p.x(), p.y());
            r.scale(p.scale());
            switch (t.kind()) {
                case PLAYER -> drawPlayer(r, t);
                case MOB -> drawMob(r, t);
                case ITEM -> drawItem(r, t);
                case TNT -> drawTnt(r, t);
            }
            r.pop();
        }
    }

    // ---- Zeilen ----

    /** A coloured piece of text. */
    private record Part(String text, int color) {}

    private static final int GRAY = 0xFFAAAAAA, GOLD = 0xFFFFB02E;

    /** Draws a row centred, bottom edge at y; returns the top edge. */
    private float line(Renderer r, List<Part> parts, float y) {
        int gap = 4;
        int width = 0;
        for (Part p : parts) width += r.textWidth(p.text);
        width += gap * (parts.size() - 1);
        int fh = r.fontHeight();
        float x1 = -width / 2f - 4, x2 = width / 2f + 4, top = y - fh - 3;
        int bg = background.argb();
        r.fill(x1 + 1, top, x2 - 1, y, bg); // abgerundete Ecken
        r.fill(x1, top + 1, x1 + 1, y - 1, bg);
        r.fill(x2 - 1, top + 1, x2, y - 1, bg);
        float x = -width / 2f;
        for (Part p : parts) {
            r.text(p.text, x, top + 2, p.color, true);
            x += r.textWidth(p.text) + gap;
        }
        return top;
    }

    private static int healthColor(float health, float max) {
        return Colors.health(max <= 0 ? 1 : Math.max(0, Math.min(1, health / max)));
    }

    private static String number(float v) {
        return v % 1 == 0 ? String.valueOf((int) v) : String.format(Locale.ROOT, "%.1f", v);
    }

    private void drawPlayer(Renderer r, NametagTarget t) {
        List<Part> parts = new ArrayList<>();
        if (gameMode.on() && !t.gameMode().isEmpty()) parts.add(new Part(t.gameMode(), GRAY));
        parts.add(new Part(t.name(), t.nameColor()));
        if (health.on()) {
            String hp = number(t.health() + t.absorption()) + " ❤";
            parts.add(new Part(hp, t.absorption() > 0 ? GOLD : healthColor(t.health(), t.maxHealth())));
        }
        if (ping.on() && t.ping() >= 0) {
            int c = t.ping() < 80 ? 0xFF55FF55 : t.ping() < 160 ? 0xFFFFFF55 : t.ping() < 300 ? 0xFFFFAA00 : 0xFFFF5555;
            parts.add(new Part(t.ping() + "ms", c));
        }
        if (distance.on()) parts.add(new Part(Math.round(t.distance()) + "m", GRAY));
        float top = line(r, parts, 0);
        if (gear.on()) drawGear(r, t.items(), top - 2);
    }

    /** Equipment above the name row: icons, durability below, enchantments above. */
    private void drawGear(Renderer r, List<ItemRef> list, float bottom) {
        List<ItemRef> shown = new ArrayList<>();
        for (ItemRef it : list) if (!skipEmpty.on() || !it.isEmpty()) shown.add(it);
        if (shown.isEmpty() || shown.stream().allMatch(ItemRef::isEmpty)) return;
        int cell = ICON + 2;
        float x0 = -(shown.size() * cell - 2) / 2f;
        boolean text = durability.get() == Durability.PERCENT || durability.get() == Durability.NUMBER;
        float iconY = bottom - ICON - (text ? 6 : 0);
        for (int i = 0; i < shown.size(); i++) {
            ItemRef it = shown.get(i);
            if (it.isEmpty()) continue;
            float x = x0 + i * cell;
            r.item(it, x, iconY, durability.get() == Durability.BAR || !it.isDamageable());
            if (text && it.isDamageable()) {
                String d = durability.get() == Durability.PERCENT ? Math.round(it.durabilityFraction() * 100) + "%" : String.valueOf(it.durability());
                small(r, d, x + ICON / 2f, iconY + ICON + 1, Colors.health(it.durabilityFraction()));
            }
            if (enchants.on()) {
                List<ItemRef.Enchant> en = it.enchantments();
                float ey = iconY - 1;
                for (int k = en.size() - 1; k >= 0; k--) {
                    ItemRef.Enchant e = en.get(k);
                    ey -= 5;
                    small(r, shortName(e), x + ICON / 2f, ey, e.curse() ? 0xFFFF5555 : 0xFFE0E0FF);
                }
            }
        }
    }

    /** "Protection" 4 → "Pro4", "Unbreaking" 3 → "Unb3". */
    private static String shortName(ItemRef.Enchant e) {
        String n = e.name().replace(" ", "");
        String s = n.length() <= 3 ? n : n.substring(0, 3);
        return e.level() > 1 ? s + e.level() : s;
    }

    /** Small text (half size), centred. */
    private static void small(Renderer r, String text, float cx, float y, int color) {
        r.push();
        r.translate(cx, y);
        r.scale(0.5f);
        r.text(text, -r.textWidth(text) / 2f, 0, color, true);
        r.pop();
    }

    private void drawMob(Renderer r, NametagTarget t) {
        List<Part> parts = new ArrayList<>();
        parts.add(new Part(t.name(), 0xFFFFFFFF));
        if (health.on()) parts.add(new Part(number(t.health()) + " ❤", healthColor(t.health(), t.maxHealth())));
        if (distance.on()) parts.add(new Part(Math.round(t.distance()) + "m", GRAY));
        float top = line(r, parts, 0);
        if (gear.on()) drawGear(r, t.items(), top - 2); // only if the mob wears something
    }

    private void drawItem(Renderer r, NametagTarget t) {
        List<Part> parts = new ArrayList<>();
        parts.add(new Part(t.name(), t.nameColor()));
        if (itemCount.on() && t.count() > 1) parts.add(new Part("×" + t.count(), GOLD));
        line(r, parts, 0);
    }

    private void drawTnt(Renderer r, NametagTarget t) {
        float sec = t.fuseTicks() / 20f;
        int c = sec > 2 ? 0xFF55FF55 : sec > 1 ? 0xFFFFAA00 : 0xFFFF5555;
        line(r, List.of(new Part(Lang.tr("aio.ui.nametags.tnt", "TNT"), 0xFFFFFFFF),
                new Part(String.format(Locale.ROOT, "%.1fs", sec), c)), 0);
    }
}
