package dev.aio.core.modules.performance;

import dev.aio.core.event.Events;
import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.Setting;
import dev.aio.core.setting.StringSetting;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * FPS-Boost: stellt die teuersten Vanilla-Einstellungen auf schnell, ohne dass das Spiel schlechter
 * wirkt. Am meisten bringt Biom-Blending aus: Beim Fliegen (gerade durch Höhlen) baut Minecraft
 * ständig Chunk-Abschnitte neu, und das Mischen der Biomfarben ist dabei der größte Brocken.
 * Ausschalten stellt die vorherigen Werte wieder her.
 *
 * <p>Die Diagnose schreibt alle 30 Sekunden ins Log, wie viel Zeit AIO selbst pro Frame braucht.
 */
public final class FpsBoostModule extends Module {
    public static final String BIOME_BLEND = "biomeBlend", SHADOWS = "shadows", CLOUDS = "clouds", CHUNK_UPDATES = "chunkUpdates",
            TRANSPARENCY = "transparency", FADE_IN = "fadeIn", SMOOTH_LIGHT = "smoothLight";

    private final BooleanSetting biomeBlend, shadows, clouds, chunkUpdates, transparency, fadeIn, smoothLight;
    private final BooleanSetting diagnose;
    /** Ursprüngliche Vanilla-Werte (vom Adapter kodiert), damit Ausschalten sie zurückholt. */
    private final StringSetting saved;

    public FpsBoostModule() {
        super("fpsboost", "FPS-Boost", "Teure Grafik-Einstellungen auf schnell: deutlich mehr FPS, vor allem beim Fliegen", Category.PERFORMANCE, false);
        biomeBlend = add(new BooleanSetting(BIOME_BLEND, "Biom-Blending aus (größter Gewinn)", true));
        chunkUpdates = add(new BooleanSetting(CHUNK_UPDATES, "Chunks im Hintergrund bauen", true));
        shadows = add(new BooleanSetting(SHADOWS, "Entity-Schatten aus", true));
        clouds = add(new BooleanSetting(CLOUDS, "Wolken aus", true));
        transparency = add(new BooleanSetting(TRANSPARENCY, "Verbesserte Transparenz aus", true));
        fadeIn = add(new BooleanSetting(FADE_IN, "Chunk-Einblenden aus", true));
        smoothLight = add(new BooleanSetting(SMOOTH_LIGHT, "Weiche Beleuchtung aus (mehr FPS, kantiger)", false));
        diagnose = add(new BooleanSetting("diagnose", "Leistungs-Diagnose ins Log", true));
        saved = add(new StringSetting("saved", "Gespeicherte Werte", "", 4000));
        saved.hidden();
        for (Setting<?> s : new Setting<?>[]{biomeBlend, shadows, clouds, chunkUpdates, transparency, fadeIn, smoothLight}) {
            s.onChange(v -> { if (isActive()) apply(); });
        }
        // Optionen gibt es erst, wenn das Spiel geladen ist: beim Betreten einer Welt (erneut) anwenden
        on(Events.ServerJoin.class, e -> apply());
    }

    @Override public boolean enabledByDefault() { return true; }

    @Override protected void onActivate() { apply(); }

    @Override protected void onDeactivate() {
        platform().restoreFpsBoost(saved.get());
        saved.set("");
    }

    private void apply() {
        Set<String> tweaks = new LinkedHashSet<>();
        if (biomeBlend.on()) tweaks.add(BIOME_BLEND);
        if (shadows.on()) tweaks.add(SHADOWS);
        if (clouds.on()) tweaks.add(CLOUDS);
        if (chunkUpdates.on()) tweaks.add(CHUNK_UPDATES);
        if (transparency.on()) tweaks.add(TRANSPARENCY);
        if (fadeIn.on()) tweaks.add(FADE_IN);
        if (smoothLight.on()) tweaks.add(SMOOTH_LIGHT);
        String next = platform().applyFpsBoost(tweaks, saved.get());
        if (next != null) saved.set(next);
    }

    public boolean diagnose() { return isActive() && diagnose.on(); }
}
