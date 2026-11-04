package dev.aio.core.platform;

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
 */
public record LookTarget(Kind kind, String name, String source, ItemRef icon, Boolean correctTool, String toolType,
                         float growth, float health, float maxHealth, int armor, int x, int y, int z) {
    public enum Kind { BLOCK, ENTITY }

    public static LookTarget block(String name, String source, ItemRef icon, Boolean correctTool, String toolType,
                                   float growth, int x, int y, int z) {
        return new LookTarget(Kind.BLOCK, name, source, icon, correctTool, toolType, growth, Float.NaN, Float.NaN, -1, x, y, z);
    }

    public static LookTarget entity(String name, String source, ItemRef icon, float health, float maxHealth, int armor,
                                    int x, int y, int z) {
        return new LookTarget(Kind.ENTITY, name, source, icon, null, null, Float.NaN, health, maxHealth, armor, x, y, z);
    }
}
