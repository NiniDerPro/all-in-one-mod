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
    private static final int LINE = 10;

    private final BooleanSetting colorByType;

    private List<String> lines = List.of();
    private List<Integer> colors = List.of();
    private int cachedW = -1;

    public PotionStatusModule() {
        super("potions", "Trank-Effekte", "Aktive Effekte mit Stufe und Restzeit", 1.0, 0.0);
        colorByType = add(new BooleanSetting("colorByType", "Farbe nach Effektart", false));
    }

    @Override protected void onActivate() { update(); }

    @Override public void update() {
        List<String> nextLines = new ArrayList<>();
        List<Integer> nextColors = new ArrayList<>();
        if (platform().inWorld()) {
            for (EffectInfo e : platform().effects()) {
                nextLines.add(e.name() + " " + roman(e.amplifier()) + "  " + formatDuration(e));
                nextColors.add(!colorByType.on() ? textColor() : e.beneficial() ? 0xFF8CF08C : 0xFFFF7070);
            }
        }
        if (!nextLines.equals(lines) || !nextColors.equals(colors)) {
            lines = nextLines;
            colors = nextColors;
            cachedW = -1;
        }
    }

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
            cachedW = max + PADDING * 2;
        }
        return cachedW;
    }

    @Override public int height(Renderer r) {
        return PADDING * 2 + Math.max(1, shown(true).size()) * LINE - 2;
    }

    @Override protected void renderContent(Renderer r, boolean preview) {
        List<String> s = shown(preview);
        for (int i = 0; i < s.size(); i++) {
            if (colorByType.on() && i < colors.size() && !lines.isEmpty()) {
                r.text(s.get(i), PADDING, PADDING + i * LINE, colors.get(i), shadow());
            } else {
                drawText(r, s.get(i), PADDING, PADDING + i * LINE);
            }
        }
    }
}
