package dev.aio.core.modules.info;

import dev.aio.core.hud.TextHudElement;
import dev.aio.core.module.Category;
import dev.aio.core.setting.BooleanSetting;

/** Uhrzeit in der Welt, optional mit Tag/Nacht-Symbol. */
public final class WorldTimeModule extends TextHudElement {
    private final BooleanSetting twelveHour;
    private final BooleanSetting symbol;

    public WorldTimeModule() {
        super("worldtime", "Uhrzeit (Welt)", "Uhrzeit in der Minecraft-Welt", Category.INFO, false, 0.0, 0.36);
        twelveHour = add(new BooleanSetting("twelveHour", "12-Stunden-Format", false));
        symbol = add(new BooleanSetting("symbol", "Sonne/Mond", true));
    }

    @Override protected String getText() {
        if (!platform().inWorld()) return "";
        long t = platform().dayTime();
        String time = WorldTime.format(t, twelveHour.on());
        return symbol.on() ? (WorldTime.isNight(t) ? "☾ " : "☀ ") + time : time;
    }

    @Override protected String previewText() { return "☀ 13:37"; }
}
