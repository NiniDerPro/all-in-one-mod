package dev.aio.core.platform;

/**
 * Aktiver Statuseffekt. {@code amplifier} 0 = Stufe I.
 *
 * @param iconSprite sprite id of the original effect icon, e.g. {@code minecraft:mob_effect/speed}
 */
public record EffectInfo(String name, int amplifier, int durationTicks, boolean infinite, boolean beneficial, String iconSprite) {
    public EffectInfo(String name, int amplifier, int durationTicks, boolean infinite, boolean beneficial) {
        this(name, amplifier, durationTicks, infinite, beneficial, "");
    }
}
