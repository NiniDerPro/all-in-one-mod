package dev.aio.core.module;

public enum Category {
    HUD("HUD", 0xFF4FC3FF),
    INFO("Info", 0xFF3DDBD9),
    NAVIGATION("Navigation", 0xFF8BE24C),
    RENDER("Grafik", 0xFFFFB02E),
    INVENTORY("Inventar", 0xFFFF8A4C),
    COMBAT("Spieler & Kampf", 0xFFFF5370),
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
