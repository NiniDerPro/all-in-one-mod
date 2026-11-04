package dev.aio.core.platform;

/** Daten zum aktuellen Server. Felder können leer sein, wenn sie nicht bekannt sind. */
public record ServerInfo(String name, String address, int ping, int players, String version, String motd, String brand) {}
