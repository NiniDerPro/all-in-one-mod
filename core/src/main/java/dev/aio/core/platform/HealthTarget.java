package dev.aio.core.platform;

/** Ein Lebewesen mit Lebensanzeige: Punkt über dem Kopf (über dem Namensschild), Leben und Typ. */
public record HealthTarget(double x, double y, double z, double distance, float health, float maxHealth,
                           float absorption, boolean player) {}
