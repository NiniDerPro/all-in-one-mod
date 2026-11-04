package dev.aio.core.module;

import java.util.Map;

/** Kategorien im Menü, jeweils mit Farbe und Item-Symbol. */
public enum Category {
    HUD("HUD", 0xFF4FC3FF, "minecraft:item_frame"),
    QOL("QoL", 0xFF8BE24C, "minecraft:name_tag"),
    PVP("PvP", 0xFFFF5370, "minecraft:diamond_sword"),
    VISUALS("Visuals", 0xFFFFB02E, "minecraft:spyglass"),
    PERFORMANCE("Performance", 0xFF5CE1A0, "minecraft:redstone"),
    CLIENT("Client", 0xFFA98BFF, "minecraft:comparator");

    private final String displayName;
    private final int color;
    private final String icon;

    Category(String displayName, int color, String icon) {
        this.displayName = displayName;
        this.color = color;
        this.icon = icon;
    }

    public String displayName() { return displayName; }

    /** Eigene Farbe der Kategorie für Menü und Karten. */
    public int color() { return color; }

    /** Item-ID des Symbols in der Seitenleiste. */
    public String icon() { return icon; }

    /** Feste Zuordnung Modul → Kategorie (eine Stelle für die ganze Aufteilung). */
    private static final Map<String, Category> BY_MODULE = Map.ofEntries(
            // HUD: reine Anzeigen
            Map.entry("fps", HUD), Map.entry("armorhud", HUD), Map.entry("potions", HUD), Map.entry("coordinates", HUD),
            Map.entry("ping", HUD), Map.entry("biome", HUD), Map.entry("daycounter", HUD), Map.entry("worldtime", HUD),
            Map.entry("playtime", HUD), Map.entry("serverinfo", HUD), Map.entry("compass", HUD), Map.entry("helditeminfo", HUD),
            // PvP
            Map.entry("cps", PVP), Map.entry("keystrokes", PVP), Map.entry("totems", PVP), Map.entry("arrows", PVP),
            Map.entry("autoweapon", PVP), Map.entry("togglesprint", PVP), Map.entry("nohurtcam", PVP),
            // QoL
            Map.entry("lookat", QOL), Map.entry("foodinfo", QOL), Map.entry("shulkerpreview", QOL), Map.entry("invtweaks", QOL),
            Map.entry("itemlist", QOL), Map.entry("elytraswap", QOL), Map.entry("autotool", QOL), Map.entry("villagerinfo", QOL),
            Map.entry("keybinds", QOL), Map.entry("namealert", QOL), Map.entry("chat", QOL), Map.entry("waypoints", QOL),
            Map.entry("deathpoint", QOL), Map.entry("portalcalc", QOL),
            // Visuals
            Map.entry("fullbright", VISUALS), Map.entry("nofog", VISUALS), Map.entry("nopumpkin", VISUALS),
            Map.entry("blockoutline", VISUALS), Map.entry("zoom", VISUALS), Map.entry("freelook", VISUALS),
            Map.entry("freecam", VISUALS));

    /** Kategorie für ein Modul: feste Zuordnung, sonst die im Modul angegebene. */
    public static Category of(String moduleId, Category fallback) { return BY_MODULE.getOrDefault(moduleId, fallback); }
}
