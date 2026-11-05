package dev.aio.core.modules.performance;

import dev.aio.core.i18n.Lang;

import dev.aio.core.event.Events;
import dev.aio.core.hud.HudElement;
import dev.aio.core.module.Category;
import dev.aio.core.platform.Renderer;
import dev.aio.core.setting.StringSetting;

/**
 * Das Profiler-Kreisdiagramm aus F3+1 als eigenes HUD-Element: ohne F3, frei verschieb- und
 * skalierbar. Solange das Modul läuft, sammelt Minecraft die Profiler-Daten (kostet etwas Leistung).
 * Ist F3 offen, zeigt Minecraft das Original und dieses Element pausiert, damit nichts doppelt ist.
 * Die Ebene, die man dort mit F3+1 und den Zahlentasten auswählt, übernimmt das Element und merkt
 * sie sich auch über einen Neustart hinweg.
 */
public final class ProfilerChartModule extends HudElement {
    /** Breite des Vanilla-Diagramms; die Höhe hängt von der Zahl der Einträge ab (9 px je Zeile). */
    public static final int W = 270;

    static int heightFor(int entries) { return 160 + 9 * entries; }

    /** Gewählte Ebene aus F3+1 ("root", "root.gameRenderer", ...). */
    private final StringSetting path;

    public ProfilerChartModule() {
        super("piechart", "Profiler Chart", "The F3 + 1 pie chart (what costs frame time) without opening F3. "
                + "Pick the section in F3 + 1 with the number keys, the overlay keeps it",
                Category.PERFORMANCE, false, 1.0, 1.0);
        path = add(new StringSetting("path", "Section", "root", 512));
        path.hidden();
        on(Events.Tick.class, e -> syncPath());
    }

    /** Bei offenem F3 die dort gewählte Ebene übernehmen, sonst die gespeicherte ins Diagramm setzen. */
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

    /** Ob Minecraft die Profiler-Daten sammeln soll. */
    public boolean collecting() { return isActive(); }
}
