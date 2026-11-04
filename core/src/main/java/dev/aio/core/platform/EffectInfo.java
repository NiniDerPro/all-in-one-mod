package dev.aio.core.platform;

/** Aktiver Statuseffekt. {@code amplifier} 0 = Stufe I. */
public record EffectInfo(String name, int amplifier, int durationTicks, boolean infinite, boolean beneficial) {}
