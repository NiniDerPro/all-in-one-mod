package dev.aio.fabric;

import dev.aio.core.modules.performance.FpsBoostModule;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.PrioritizeChunkUpdates;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Setzt die Vanilla-Optionen für den FPS-Boost und merkt sich die Ursprungswerte
 * ("schlüssel=wert;…"), damit Ausschalten sie zurückholt.
 */
final class FpsBoost {
    private FpsBoost() {}

    private static final String[] KEYS = {FpsBoostModule.BIOME_BLEND, FpsBoostModule.SHADOWS, FpsBoostModule.CLOUDS,
            FpsBoostModule.CHUNK_UPDATES, FpsBoostModule.TRANSPARENCY, FpsBoostModule.FADE_IN, FpsBoostModule.SMOOTH_LIGHT};

    @SuppressWarnings("unchecked")
    private static OptionInstance<Object> option(Options o, String key) {
        return (OptionInstance<Object>) (OptionInstance<?>) switch (key) {
            case FpsBoostModule.BIOME_BLEND -> o.biomeBlendRadius();
            case FpsBoostModule.SHADOWS -> o.entityShadows();
            case FpsBoostModule.CLOUDS -> o.cloudStatus();
            case FpsBoostModule.CHUNK_UPDATES -> o.prioritizeChunkUpdates();
            case FpsBoostModule.TRANSPARENCY -> o.improvedTransparency();
            case FpsBoostModule.FADE_IN -> o.chunkSectionFadeInTime();
            case FpsBoostModule.SMOOTH_LIGHT -> o.ambientOcclusion();
            default -> throw new IllegalArgumentException(key);
        };
    }

    /** Schneller Wert je Optimierung. */
    private static Object fast(String key) {
        return switch (key) {
            case FpsBoostModule.BIOME_BLEND -> 0;
            case FpsBoostModule.CLOUDS -> CloudStatus.OFF;
            case FpsBoostModule.CHUNK_UPDATES -> PrioritizeChunkUpdates.NONE;
            case FpsBoostModule.FADE_IN -> 0.0;
            default -> Boolean.FALSE; // Schatten, Transparenz, weiche Beleuchtung
        };
    }

    private static Object parse(String key, String text) {
        Object sample = fast(key);
        if (sample instanceof Integer) return Integer.parseInt(text);
        if (sample instanceof Double) return Double.parseDouble(text);
        if (sample instanceof CloudStatus) return CloudStatus.valueOf(text);
        if (sample instanceof PrioritizeChunkUpdates) return PrioritizeChunkUpdates.valueOf(text);
        return Boolean.parseBoolean(text);
    }

    private static String encode(Object v) { return v instanceof Enum<?> e ? e.name() : String.valueOf(v); }

    static Map<String, String> decode(String saved) {
        Map<String, String> out = new LinkedHashMap<>();
        if (saved == null || saved.isBlank()) return out;
        for (String part : saved.split(";")) {
            int eq = part.indexOf('=');
            if (eq > 0) out.put(part.substring(0, eq), part.substring(eq + 1));
        }
        return out;
    }

    static String apply(Minecraft client, Set<String> tweaks, String saved) {
        Options o = client.options;
        if (o == null || client.levelRenderer == null) return null;
        Map<String, String> originals = decode(saved);
        boolean changed = false;
        for (String key : KEYS) {
            try {
                OptionInstance<Object> opt = option(o, key);
                if (tweaks.contains(key)) {
                    Object target = fast(key);
                    if (!originals.containsKey(key)) originals.put(key, encode(opt.get()));
                    if (!target.equals(opt.get())) {
                        opt.set(target);
                        changed = true;
                    }
                } else if (originals.containsKey(key)) {
                    opt.set(parse(key, originals.remove(key)));
                    changed = true;
                }
            } catch (RuntimeException e) {
                AioFabricClient.LOGGER.warn("FPS-Boost: Option {} nicht setzbar: {}", key, e.toString());
            }
        }
        if (changed) o.save();
        StringBuilder sb = new StringBuilder();
        originals.forEach((k, v) -> sb.append(sb.isEmpty() ? "" : ";").append(k).append('=').append(v));
        return sb.toString();
    }

    static void restore(Minecraft client, String saved) {
        Options o = client.options;
        if (o == null) return;
        decode(saved).forEach((key, value) -> {
            try {
                option(o, key).set(parse(key, value));
            } catch (RuntimeException e) {
                AioFabricClient.LOGGER.warn("FPS-Boost: {} nicht zurücksetzbar: {}", key, e.toString());
            }
        });
        o.save();
    }
}
