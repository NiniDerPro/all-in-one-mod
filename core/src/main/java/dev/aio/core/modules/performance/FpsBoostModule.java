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
            TRANSPARENCY = "transparency", FADE_IN = "fadeIn", SMOOTH_LIGHT = "smoothLight",
            WEATHER = "weather", VIGNETTE = "vignette", MENU_BLUR = "menuBlur", SOLID_LEAVES = "solidLeaves",
            MIPMAPS = "mipmaps", ENTITY_DISTANCE = "entityDistance";

    private final BooleanSetting biomeBlend, shadows, clouds, chunkUpdates, transparency, fadeIn, smoothLight;
    private final BooleanSetting weather, vignette, menuBlur, solidLeaves, mipmaps, entityDistance;
    private final BooleanSetting diagnose;
    /** Ursprüngliche Vanilla-Werte (vom Adapter kodiert), damit Ausschalten sie zurückholt. */
    private final StringSetting saved;

    public FpsBoostModule() {
        super("fpsboost", "FPS Boost", "Sets expensive graphics options to fast: much higher FPS, especially while flying", Category.PERFORMANCE, false);
        biomeBlend = add(new BooleanSetting(BIOME_BLEND, "Biome blend off (biggest gain)", true));
        chunkUpdates = add(new BooleanSetting(CHUNK_UPDATES, "Build chunks in the background", true));
        shadows = add(new BooleanSetting(SHADOWS, "Entity shadows off", true));
        clouds = add(new BooleanSetting(CLOUDS, "Clouds off", true));
        transparency = add(new BooleanSetting(TRANSPARENCY, "Improved transparency off", true));
        fadeIn = add(new BooleanSetting(FADE_IN, "Chunk fade-in off", true));
        smoothLight = add(new BooleanSetting(SMOOTH_LIGHT, "Smooth lighting off (more FPS, blockier)", false));
        weather = add(new BooleanSetting(WEATHER, "Smaller rain/snow radius", true));
        vignette = add(new BooleanSetting(VIGNETTE, "Vignette (dark edges) off", true));
        menuBlur = add(new BooleanSetting(MENU_BLUR, "Menu blur off", true));
        solidLeaves = add(new BooleanSetting(SOLID_LEAVES, "Opaque leaves (lots of FPS in forests)", false));
        mipmaps = add(new BooleanSetting(MIPMAPS, "Mipmaps off (reloads textures)", false));
        entityDistance = add(new BooleanSetting(ENTITY_DISTANCE, "Entity view distance 75 %", false));
        diagnose = add(new BooleanSetting("diagnose", "Performance report in the log", true));
        saved = add(new StringSetting("saved", "Saved values", "", 4000));
        saved.hidden();
        for (Setting<?> s : new Setting<?>[]{biomeBlend, shadows, clouds, chunkUpdates, transparency, fadeIn, smoothLight,
                weather, vignette, menuBlur, solidLeaves, mipmaps, entityDistance}) {
            s.onChange(v -> { if (isActive()) apply(); });
        }
        // Optionen gibt es erst, wenn das Spiel geladen ist: beim Betreten einer Welt (erneut) anwenden
        on(Events.ServerJoin.class, e -> apply());
    }


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
        if (weather.on()) tweaks.add(WEATHER);
        if (vignette.on()) tweaks.add(VIGNETTE);
        if (menuBlur.on()) tweaks.add(MENU_BLUR);
        if (solidLeaves.on()) tweaks.add(SOLID_LEAVES);
        if (mipmaps.on()) tweaks.add(MIPMAPS);
        if (entityDistance.on()) tweaks.add(ENTITY_DISTANCE);
        String next = platform().applyFpsBoost(tweaks, saved.get());
        if (next != null) saved.set(next);
    }

    public boolean diagnose() { return isActive() && diagnose.on(); }
}
