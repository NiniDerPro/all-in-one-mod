package dev.aio.core.platform;

import java.util.List;

/**
 * What the player is currently looking at (block or entity under the crosshair).
 * The adapter only delivers raw data, the core decides how it is shown.
 *
 * @param name        angezeigter Name, z. B. "Grasblock"
 * @param source      name of the mod the target comes from, e.g. "Minecraft"
 * @param icon        Item-Icon (bei Wesen meist leer)
 * @param correctTool true = matching tool in hand, false = no drop without a matching tool, null = neutral
 * @param toolType    required tool type ("Pickaxe", ...) or null
 * @param growth      growth 0..1 for plants, otherwise NaN
 * @param health      health points for entities, otherwise NaN
 * @param armor       armor points for entities, otherwise -1
 * @param details     block-specific details (honey in a beehive, fill level in a composter, ...).
 *                    The adapter only delivers those that fit the viewed block type.
 */
public record LookTarget(Kind kind, String name, String source, ItemRef icon, Boolean correctTool, String toolType,
                         float growth, float health, float maxHealth, int armor, int x, int y, int z,
                         List<Detail> details, String typeName, ItemRef toolIcon) {
    public enum Kind { BLOCK, ENTITY }

    /**
     * Eine Zeile "Bezeichnung: Wert".
     *
     * @param highlight highlight the value (green), e.g. "Honey: 5/5" = ready to harvest
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

    /** With tool icon (the weakest tool that breaks the block), next to the name. */
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

    /** @param typeName kind of the entity ("Zombie"), important when {@code name} is a name tag */
    public static LookTarget entity(String name, String typeName, String source, ItemRef icon, float health, float maxHealth,
                                    int armor, int x, int y, int z) {
        return new LookTarget(Kind.ENTITY, name, source, icon, null, null, Float.NaN, health, maxHealth, armor, x, y, z,
                List.of(), typeName, ItemRef.EMPTY);
    }
}
