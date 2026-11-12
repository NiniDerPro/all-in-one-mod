package dev.aio.core.platform;

/** Data about the current server. Fields can be empty when they are not known. */
public record ServerInfo(String name, String address, int ping, int players, String version, String motd, String brand) {}
