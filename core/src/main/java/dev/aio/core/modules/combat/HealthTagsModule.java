package dev.aio.core.modules.combat;

import dev.aio.core.hud.HudElement;
import dev.aio.core.module.Category;
import dev.aio.core.modules.navigation.Projection;
import dev.aio.core.modules.render.NametagsModule;
import dev.aio.core.platform.CameraView;
import dev.aio.core.platform.HealthTarget;
import dev.aio.core.platform.NametagTarget;
import dev.aio.core.platform.Renderer;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.EnumSetting;
import dev.aio.core.setting.NumberSetting;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Health Indicator: hearts above players (and optionally mobs), like the HealthIndicator mod.
 * With the original hearts, absorption in gold. Many hearts (bosses) are shown as a number.
 */
public final class HealthTagsModule extends HudElement {
    public enum Style implements EnumSetting.Named {
        HEARTS("Hearts"), NUMBER("Number"), BOTH("Hearts and number");

        private final String displayName;

        Style(String displayName) { this.displayName = displayName; }

        @Override public String displayName() { return displayName; }
    }

    private static final String FULL = "minecraft:hud/heart/full", HALF = "minecraft:hud/heart/half",
            CONTAINER = "minecraft:hud/heart/container", ABS_FULL = "minecraft:hud/heart/absorbing_full",
            ABS_HALF = "minecraft:hud/heart/absorbing_half";

    private final BooleanSetting players, hostile, passive, onlyDamaged, throughWalls;
    private final EnumSetting<Style> style;
    private final NumberSetting distance, size;

    public HealthTagsModule() {
        super("healthtags", "Health Indicator", "Hearts above players and mobs, like the HealthIndicator mod", Category.PVP, false, 0, 0);
        hideBoxSettings();
        players = add(new BooleanSetting("players", "Players", true));
        hostile = add(new BooleanSetting("hostile", "Hostile mobs", false));
        passive = add(new BooleanSetting("passive", "Other mobs", false));
        onlyDamaged = add(new BooleanSetting("onlyDamaged", "Only when damaged", false));
        throughWalls = add(new BooleanSetting("walls", "Show through walls", false));
        style = add(new EnumSetting<>("style", "Style", Style.HEARTS));
        distance = add(new NumberSetting("distance", "Max distance", 32, 8, 96, 4, " blocks"));
        size = add(new NumberSetting("size", "Size", 100, 50, 200, 10, " %"));
    }

    @Override public int width(Renderer r) { return 1; }

    @Override public int height(Renderer r) { return 1; }

    @Override public boolean editable() { return false; }

    @Override protected boolean drawsDefaultBackground() { return false; }

    @Override protected void renderContent(Renderer r, boolean preview) {}

    @Override public boolean hasContent() { return platform().inWorld() && !platform().isScreenOpen(); }

    @Override public void renderOverlay(Renderer r, float partialTick) {
        if (!isActive() || !hasContent()) return;
        // If the nametags already show the health of players, no second display above them here
        NametagsModule tags = core.module(NametagsModule.class);
        boolean playersHere = players.on() && (tags == null || !tags.replaces(NametagTarget.Kind.PLAYER));
        List<HealthTarget> list = new ArrayList<>(platform().healthTargets(distance.get(), partialTick,
                playersHere, hostile.on(), passive.on(), throughWalls.on()));
        if (list.isEmpty()) return;
        list.sort((a, b) -> Double.compare(b.distance(), a.distance())); // nahe zuletzt (oben)
        CameraView cam = platform().camera(partialTick);
        float w = r.screenWidth(), h = r.screenHeight();
        float base = (float) (size.get() / 100.0);
        for (HealthTarget t : list) {
            if (onlyDamaged.on() && t.health() >= t.maxHealth() && t.absorption() <= 0) continue;
            Projection.Point pt = Projection.project(t.x(), t.y(), t.z(), cam, w, h);
            if (!pt.onScreen()) continue;
            // keeps its size relative to the world, like vanilla nametags
            float scale = base * Projection.worldScale(t.distance(), cam, h);
            r.push();
            r.translate(pt.x(), pt.y());
            r.scale(scale);
            draw(r, t);
            r.pop();
        }
    }

    /** Draws above the point (0,0), flush at the bottom. */
    private void draw(Renderer r, HealthTarget t) {
        int hearts = (int) Math.ceil(t.maxHealth() / 2f);
        int absHearts = (int) Math.ceil(t.absorption() / 2f);
        boolean tooMany = hearts + absHearts > 20;
        String number = String.format(Locale.ROOT, t.health() % 1 == 0 ? "%.0f" : "%.1f", t.health())
                + (t.absorption() > 0 ? String.format(Locale.ROOT, " +%.0f", t.absorption()) : "") + " ❤";
        boolean showHearts = style.get() != Style.NUMBER && !tooMany;
        boolean showNumber = style.get() != Style.HEARTS || tooMany;
        float y = 0;
        if (showNumber) {
            float tw = r.textWidth(number);
            y -= r.fontHeight();
            r.fill(-tw / 2 - 2, y - 1, tw / 2 + 2, y + r.fontHeight() - 1, 0x60000000);
            int color = t.health() <= t.maxHealth() * 0.25f ? 0xFFFF5555 : t.health() <= t.maxHealth() * 0.5f ? 0xFFFFAA00 : 0xFFFFFFFF;
            r.text(number, -tw / 2, y, color, true);
            y -= 1;
        }
        if (showHearts) {
            int total = hearts + absHearts;
            int perRow = Math.min(10, total);
            int rows = (total + 9) / 10;
            float rowW = perRow * 8 + 1;
            for (int i = 0; i < total; i++) {
                int row = i / 10, col = i % 10;
                float hx = -rowW / 2 + col * 8, hy = y - 9 - (rows - 1 - row) * 9;
                r.sprite(CONTAINER, hx, hy, 9, 9);
                if (i < hearts) {
                    float hp = t.health() - i * 2;
                    if (hp >= 2) r.sprite(FULL, hx, hy, 9, 9);
                    else if (hp > 0) r.sprite(HALF, hx, hy, 9, 9);
                } else {
                    float ab = t.absorption() - (i - hearts) * 2;
                    if (ab >= 2) r.sprite(ABS_FULL, hx, hy, 9, 9);
                    else if (ab > 0) r.sprite(ABS_HALF, hx, hy, 9, 9);
                }
            }
        }
    }
}
