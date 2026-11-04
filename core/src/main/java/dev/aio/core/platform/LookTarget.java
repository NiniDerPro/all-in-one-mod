package dev.aio.core.platform;

import java.util.List;

/**
 * Was der Spieler gerade ansieht (Block oder Wesen unter dem Fadenkreuz).
 * Der Adapter liefert nur Rohdaten, die Darstellung entscheidet der Kern.
 *
 * @param name        angezeigter Name, z. B. "Grasblock"
 * @param source      Name der Mod, aus der das Ziel stammt, z. B. "Minecraft"
 * @param icon        Item-Icon (bei Wesen meist leer)
 * @param correctTool true = passendes Werkzeug in der Hand, false = ohne passendes Werkzeug kein Drop, null = neutral
 * @param toolType    benötigter Werkzeugtyp ("Spitzhacke", ...) oder null
 * @param growth      Wachstum 0..1 bei Pflanzen, sonst NaN
 * @param health      Lebenspunkte bei Wesen, sonst NaN
 * @param armor       Rüstungspunkte bei Wesen, sonst -1
 * @param details     blockspezifische Angaben (Honig im Bienenstock, Füllstand im Komposter, ...).
 *                    Der Adapter liefert nur die, die zur angesehenen Blockart passen.
 */
public record LookTarget(Kind kind, String name, String source, ItemRef icon, Boolean correctTool, String toolType,
                         float growth, float health, float maxHealth, int armor, int x, int y, int z,
                         List<Detail> details, String typeName, ItemRef toolIcon) {
    public enum Kind { BLOCK, ENTITY }

    /**
     * Eine Zeile "Bezeichnung: Wert".
     *
     * @param highlight Wert hervorheben (grün), z. B. "Honig: 5/5" = erntebereit
     */
    public record Detail(String label, String value, boolean highlight) {
        public static Detail of(String label, String value) { return new Detail(label, value, false); }

        public static Detail of(String label, int value, int max) {
            return new Detail(label, value + "/" + max, value >= max);
        }
    }

    public LookTarget {
        details = details == null ? List.of() : List.copyOf(details);
        typeName = typeName == null ? "" : typeName;
        toolIcon = toolIcon == null ? ItemRef.EMPTY : toolIcon;
    }

    /** Mit Werkzeug-Symbol (das schwächste Werkzeug, das den Block abbaut), wie bei Jade neben dem Namen. */
    public LookTarget withToolIcon(ItemRef tool) {
        return new LookTarget(kind, name, source, icon, correctTool, toolType, growth, health, maxHealth, armor, x, y, z,
                details, typeName, tool);
    }

    public static LookTarget block(String name, String source, ItemRef icon, Boolean correctTool, String toolType,
                                   float growth, int x, int y, int z, List<Detail> details) {
        return new LookTarget(Kind.BLOCK, name, source, icon, correctTool, toolType, growth, Float.NaN, Float.NaN, -1,
                x, y, z, details, "", ItemRef.EMPTY);
    }

    public static LookTarget block(String name, String source, ItemRef icon, Boolean correctTool, String toolType,
                                   float growth, int x, int y, int z) {
        return block(name, source, icon, correctTool, toolType, growth, x, y, z, List.of());
    }

    public static LookTarget entity(String name, String source, ItemRef icon, float health, float maxHealth, int armor,
                                    int x, int y, int z) {
        return entity(name, "", source, icon, health, maxHealth, armor, x, y, z);
    }

    /** @param typeName Art des Wesens ("Zombie"), wichtig wenn {@code name} ein Namensschild ist */
    public static LookTarget entity(String name, String typeName, String source, ItemRef icon, float health, float maxHealth,
                                    int armor, int x, int y, int z) {
        return new LookTarget(Kind.ENTITY, name, source, icon, null, null, Float.NaN, health, maxHealth, armor, x, y, z,
                List.of(), typeName, ItemRef.EMPTY);
    }
}
