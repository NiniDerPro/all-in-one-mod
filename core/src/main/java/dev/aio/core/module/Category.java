package dev.aio.core.module;

public enum Category {
    HUD("HUD", 0xFF4FC3FF),
    RENDER("Grafik", 0xFFFFB02E),
    PLAYER("Spieler", 0xFF5CE07A),
    CHAT("Chat", 0xFFFF6FB5),
    CLIENT("Client", 0xFFA98BFF);

    private final String displayName;
    private final int color;

    Category(String displayName, int color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String displayName() { return displayName; }

    /** Eigene Farbe der Kategorie für Menü und Karten. */
    public int color() { return color; }
}
