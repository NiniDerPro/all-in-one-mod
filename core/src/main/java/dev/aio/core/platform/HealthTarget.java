package dev.aio.core.platform;

/** A living entity with a health display: point above the head (above the nametag), health and type. */
public record HealthTarget(double x, double y, double z, double distance, float health, float maxHealth,
                           float absorption, boolean player) {}
