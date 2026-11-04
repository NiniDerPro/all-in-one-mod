package dev.aio.core.modules.hud;

import dev.aio.core.hud.HudElement;
import dev.aio.core.platform.EffectInfo;
import dev.aio.core.platform.Renderer;
import dev.aio.core.setting.BooleanSetting;

import java.util.ArrayList;
import java.util.List;

/** Aktive Trank-Effekte mit Stufe und Restzeit. */
public final class PotionStatusModule extends HudElement {
    private static final String[] ROMAN = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
    private static final int ICON = 10;

    private final BooleanSetting colorByType;
    private final BooleanSetting icons;
    private List<String> sprites = List.of();

    private List<String> lines = List.of();
    private List<Integer> colors = List.of();
    private int cachedW = -1;

    public PotionStatusModule() {
        super("potions", "Trank-Effekte", "Aktive Effekte mit Stufe und Restzeit", 1.0, 0.0);
        colorByType = add(new BooleanSetting("colorByType", "Farbe nach Effektart", false));
        icons = add(new BooleanSetting("icons", "Effekt-Icons", true));
        icons.onChange(v -> cachedW = -1);
    }

    @Override protected void onActivate() { update(); }

    @Override public void update() {
        List<String> nextLines = new ArrayList<>();
        List<Integer> nextColors = new ArrayList<>();
        List<String> nextSprites = new ArrayList<>();
        if (platform().inWorld()) {
            for (EffectInfo e : platform().effects()) {
                nextLines.add(e.name() + " " + roman(e.amplifier()) + "  " + formatDuration(e));
                nextColors.add(!colorByType.on() ? textColor() : e.beneficial() ? 0xFF8CF08C : 0xFFFF7070);
                nextSprites.add(e.iconSprite() == null ? "" : e.iconSprite());
            }
        }
        if (!nextLines.equals(lines) || !nextColors.equals(colors)) {
            lines = nextLines;
            colors = nextColors;
            sprites = nextSprites;
            cachedW = -1;
        }
    }

    private static final String[] PREVIEW_SPRITES = {"minecraft:mob_effect/strength", "minecraft:mob_effect/speed"};

    private int line() { return icons.on() ? ICON + 2 : 10; }

    private int iconSpace() { return icons.on() ? ICON + 3 : 0; }

    static String roman(int amplifier) {
        return amplifier >= 0 && amplifier < ROMAN.length ? ROMAN[amplifier] : String.valueOf(amplifier + 1);
    }

    static String formatDuration(EffectInfo e) {
        if (e.infinite()) return "∞";
        int totalSeconds = e.durationTicks() / 20;
        return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    @Override public boolean hasContent() { return !lines.isEmpty(); }

    private List<String> shown(boolean preview) {
        return lines.isEmpty() && preview ? List.of("Stärke II  1:30", "Tempo I  3:00") : lines;
    }

    @Override public int width(Renderer r) {
        if (cachedW < 0) {
            int max = 0;
            for (String s : shown(true)) max = Math.max(max, r.textWidth(s));
            cachedW = max + PADDING * 2 + iconSpace();
        }
        return cachedW;
    }

    @Override public int height(Renderer r) {
        return PADDING * 2 + Math.max(1, shown(true).size()) * line() - 2;
    }

    @Override protected void renderContent(Renderer r, boolean preview) {
        List<String> s = shown(preview);
        int lh = line();
        float tx = PADDING + iconSpace();
        for (int i = 0; i < s.size(); i++) {
            float y = PADDING + i * lh;
            float ty = y + (lh - 10) / 2f + 1;
            if (icons.on()) {
                String sprite = !lines.isEmpty() && i < sprites.size() ? sprites.get(i) : PREVIEW_SPRITES[i % PREVIEW_SPRITES.length];
                if (!sprite.isEmpty()) r.sprite(sprite, PADDING, y, ICON, ICON);
            }
            if (colorByType.on() && i < colors.size() && !lines.isEmpty()) {
                r.text(s.get(i), tx, ty, colors.get(i), shadow());
            } else {
                drawText(r, s.get(i), tx, ty);
            }
        }
    }
}
