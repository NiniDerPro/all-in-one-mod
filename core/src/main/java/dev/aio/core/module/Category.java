package dev.aio.core.module;

public enum Category {
    HUD("HUD"),
    RENDER("Grafik"),
    PLAYER("Spieler"),
    CHAT("Chat"),
    CLIENT("Client");

    private final String displayName;

    Category(String displayName) { this.displayName = displayName; }

    public String displayName() { return displayName; }
}
