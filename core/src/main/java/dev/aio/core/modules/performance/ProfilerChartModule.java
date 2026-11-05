package dev.aio.core.modules.performance;

import dev.aio.core.i18n.Lang;

import dev.aio.core.hud.HudElement;
import dev.aio.core.module.Category;
import dev.aio.core.platform.Renderer;

/**
 * Das Profiler-Kreisdiagramm aus F3+1 als eigenes HUD-Element: ohne F3, frei verschieb- und
 * skalierbar. Solange das Modul läuft, sammelt Minecraft die Profiler-Daten (kostet etwas Leistung).
 * Ist F3 offen, zeigt Minecraft das Original und dieses Element pausiert, damit nichts doppelt ist.
 */
public final class ProfilerChartModule extends HudElement {
    /** Größe des Vanilla-Diagramms (Breite fest, Höhe für etwa acht Einträge). */
    public static final int W = 270, H = 220;

    public ProfilerChartModule() {
        super("piechart", "Profiler Chart", "The F3 + 1 pie chart (what costs frame time) without opening F3",
                Category.PERFORMANCE, false, 1.0, 1.0);
    }

    @Override protected boolean defaultBackground() { return false; }

    @Override protected boolean drawsDefaultBackground() { return false; }

    @Override public int width(Renderer r) { return W; }

    @Override public int height(Renderer r) { return H; }

    @Override public boolean hasContent() { return platform().inWorld() && !platform().debugScreenOpen(); }

    @Override protected void renderContent(Renderer r, boolean preview) {
        boolean drawn = platform().drawProfilerChart(r, W, H);
        if (preview && !drawn) {
            r.fill(0, 0, W, H, 0x60000000);
            r.centeredText(Lang.tr("aio.ui.profiler_chart.profiler_chart", "Profiler Chart"), W / 2f, H / 2f - 4, 0xFFFFFFFF, true);
        }
    }

    /** Ob Minecraft die Profiler-Daten sammeln soll. */
    public boolean collecting() { return isActive(); }
}
