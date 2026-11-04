package dev.aio.core.platform;

/**
 * Eine Tastenbelegung aus Minecraft oder einer Mod.
 *
 * @param id         eindeutige ID (Übersetzungsschlüssel der Belegung)
 * @param name       Anzeigename
 * @param category   Anzeigename der Kategorie
 * @param key        MC-Tastenname, z. B. {@code key.keyboard.w}, leer = nicht belegt
 * @param defaultKey Standardtaste
 */
public record KeyBinding(String id, String name, String category, String key, String defaultKey) {
    public boolean unbound() { return Keys.isNone(key) || "key.keyboard.unknown".equals(key); }

    public boolean isDefault() { return key.equals(defaultKey); }
}
