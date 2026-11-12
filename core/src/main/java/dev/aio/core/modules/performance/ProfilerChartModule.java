package dev.aio.core.modules.performance;

import dev.aio.core.i18n.Lang;

import dev.aio.core.event.Events;
import dev.aio.core.hud.HudElement;
import dev.aio.core.module.Category;
import dev.aio.core.platform.Renderer;
import dev.aio.core.setting.StringSetting;

/**
 * The profiler pie chart from F3+1 as its own HUD element: without F3, freely movable and
 * scalable. While the module runs, Minecraft collects the profiler data (costs some performance).
 * If F3 is open, Minecraft shows the original and this element pauses so that nothing is doubled.
 * The level selected there with F3+1 and the number keys is taken over by the element, which remembers
 * it across a restart too.
 */
public final class ProfilerChartModule extends HudElement {
    /** Width of the vanilla chart; the height depends on the number of entries (9 px per row). */
    public static final int W = 270;

    static int heightFor(int entries) { return 160 + 9 * entries; }

    /** Selected level from F3+1 ("root", "root.gameRenderer", ...). */
    private final StringSetting path;

    public ProfilerChartModule() {
        super("piechart", "Profiler Chart", "The F3 + 1 pie chart (what costs frame time) without opening F3. "
                + "Pick the section in F3 + 1 with the number keys, the overlay keeps it",
                Category.PERFORMANCE, false, 1.0, 1.0);
        path = add(new StringSetting("path", "Section", "root", 512));
        path.hidden();
        on(Events.Tick.class, e -> syncPath());
    }

    /** With F3 open take over the level selected there, otherwise put the saved one into the chart. */
    private void syncPath() {
        if (platform().debugScreenOpen()) {
            String now = platform().profilerChartPath();
            if (now != null && !now.equals(path.get())) path.set(now);
        } else if (!path.get().equals(platform().profilerChartPath())) {
            platform().setProfilerChartPath(path.get().isEmpty() ? "root" : path.get());
        }
    }

    @Override protected boolean defaultBackground() { return false; }

    @Override protected boolean drawsDefaultBackground() { return false; }

    @Override public int width(Renderer r) { return W; }

    @Override public int height(Renderer r) { return heightFor(platform().profilerChartEntries()); }

    @Override public boolean hasContent() { return platform().inWorld() && !platform().debugScreenOpen(); }

    @Override protected void renderContent(Renderer r, boolean preview) {
        int h = height(r);
        boolean drawn = platform().drawProfilerChart(r, W, h);
        if (preview && !drawn) {
            r.fill(0, 0, W, h, 0x60000000);
            r.centeredText(Lang.tr("aio.ui.profiler_chart.profiler_chart", "Profiler Chart"), W / 2f, h / 2f - 4, 0xFFFFFFFF, true);
        }
    }

    /** Whether Minecraft should collect the profiler data. */
    public boolean collecting() { return isActive(); }
}
