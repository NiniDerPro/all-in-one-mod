package dev.aio.core.modules.info;

import dev.aio.core.hud.TextHudElement;
import dev.aio.core.module.Category;
import dev.aio.core.setting.BooleanSetting;

/** Uhrzeit in der Welt, optional mit Tag/Nacht-Symbol. */
public final class WorldTimeModule extends TextHudElement {
    private final BooleanSetting twelveHour;
    private final BooleanSetting symbol;

    public WorldTimeModule() {
        super("worldtime", "World Time", "Time of day in the Minecraft world", Category.QOL, false, 0.0, 0.36);
        twelveHour = add(new BooleanSetting("twelveHour", "12-hour format", false));
        symbol = add(new BooleanSetting("symbol", "Sun/moon", true));
    }

    @Override protected String getText() {
        if (!platform().inWorld()) return "";
        long t = platform().dayTime();
        String time = WorldTime.format(t, twelveHour.on());
        return symbol.on() ? (WorldTime.isNight(t) ? "☾ " : "☀ ") + time : time;
    }

    @Override protected String previewText() { return "☀ 13:37"; }
}
