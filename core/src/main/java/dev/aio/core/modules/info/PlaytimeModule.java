package dev.aio.core.modules.info;

import dev.aio.core.event.Events;
import dev.aio.core.hud.TextHudElement;
import dev.aio.core.module.Category;
import dev.aio.core.setting.BooleanSetting;

/**
 * Gesamte Spielzeit in dieser Welt laut Statistik. Die Statistik wird beim Betreten und
 * dann jede Minute angefragt (wie das Statistik-Menü); dazwischen wird lokal weitergezählt.
 */
public final class PlaytimeModule extends TextHudElement {
    private static final int REQUEST_INTERVAL = 20 * 60;

    private final BooleanSetting label;
    private long statTicks = -1;
    private long ticksSinceStat;
    private int requestTimer;

    public PlaytimeModule() {
        super("playtime", "Playtime", "Time played in this world (from statistics)", Category.INFO, false, 0.0, 0.42);
        label = add(new BooleanSetting("label", "Show label", true));
        on(Events.Tick.class, e -> tick());
        on(Events.ServerJoin.class, e -> reset());
    }

    @Override protected void onActivate() {
        reset();
        super.onActivate();
    }

    private void reset() {
        statTicks = -1;
        ticksSinceStat = 0;
        requestTimer = 40; // kurz nach dem Betreten anfragen
    }

    private void tick() {
        if (!platform().inWorld()) return;
        long stat = platform().playTimeTicks();
        if (stat >= 0 && stat != statTicks) {
            statTicks = stat;
            ticksSinceStat = 0;
        } else {
            ticksSinceStat++;
        }
        if (--requestTimer <= 0) {
            requestTimer = REQUEST_INTERVAL;
            platform().requestStats();
        }
    }

    @Override protected String getText() {
        if (!platform().inWorld() || statTicks < 0) return "";
        return (label.on() ? "Playtime: " : "") + format(statTicks + ticksSinceStat);
    }

    /** "2 T 3 Std 14 Min", "3 Std 5 Min" oder "12 Min". */
    public static String format(long ticks) {
        long minutesTotal = ticks / 20 / 60;
        long days = minutesTotal / (60 * 24);
        long hours = minutesTotal / 60 % 24;
        long minutes = minutesTotal % 60;
        if (days > 0) return days + " d " + hours + " h " + minutes + " min";
        if (hours > 0) return hours + " h " + minutes + " min";
        return minutes + " min";
    }

    @Override protected String previewText() { return "Playtime: 3 h 14 min"; }
}
