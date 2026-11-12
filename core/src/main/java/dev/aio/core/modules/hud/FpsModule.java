package dev.aio.core.modules.hud;

import dev.aio.core.event.Events;
import dev.aio.core.hud.TextHudElement;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.NumberSetting;
import dev.aio.core.util.RollingStats;

/** FPS from the MC client, optionally with average and min/max over a time window. */
public final class FpsModule extends TextHudElement {
    private final BooleanSetting showAverage;
    private final BooleanSetting showMinMax;
    private final NumberSetting window;
    private final RollingStats stats = new RollingStats(10);
    private int tickCounter;

    public FpsModule() {
        super("fps", "FPS", "Frames per second, optionally with average and min/max", 0.0, 0.0);
        showAverage = add(new BooleanSetting("showAverage", "Average", false));
        showMinMax = add(new BooleanSetting("showMinMax", "Min/Max", false));
        window = add(new NumberSetting("window", "Time window", 10, 3, 60, 1, " s"));
        window.visibleWhen(() -> showAverage.on() || showMinMax.on());
        window.onChange(v -> stats.resize(window.intValue()));
        on(Events.Tick.class, e -> sample());
    }

    @Override protected void onActivate() {
        stats.resize(window.intValue());
        stats.clear();
        tickCounter = 0;
        super.onActivate();
    }

    private void sample() {
        // MC updates the FPS value once per second, so one sample per 20 ticks is enough.
        if (++tickCounter >= 20) {
            tickCounter = 0;
            stats.add(platform().fps());
        }
    }

    @Override protected String getText() {
        StringBuilder sb = new StringBuilder().append(platform().fps()).append(" FPS");
        if (stats.size() > 0 && (showAverage.on() || showMinMax.on())) {
            sb.append(" (");
            if (showAverage.on()) sb.append("avg ").append(stats.average());
            if (showAverage.on() && showMinMax.on()) sb.append(", ");
            if (showMinMax.on()) sb.append(stats.min()).append('–').append(stats.max());
            sb.append(')');
        }
        return sb.toString();
    }

    @Override protected String previewText() { return "144 FPS"; }
}
