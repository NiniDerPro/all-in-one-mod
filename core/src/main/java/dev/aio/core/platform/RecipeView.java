package dev.aio.core.platform;

import java.util.List;

/**
 * An entry in the recipe view: a recipe, a trade, a loot list, ...
 *
 * @param type    display name of the kind, e.g. "Crafting table", "Trade", "Loot"
 * @param width   grid width (0 = no grid, list only)
 * @param slots   per input slot the possible items (empty = empty slot); several are cycled through
 * @param result  Ergebnis (EMPTY = keins)
 * @param kind    kind for transferring into the inventory ("+")
 * @param notes   text lines below the grid (level, chance, ...)
 * @param rows    list of icon and text (e.g. loot with chance), paged when needed
 * @param results for {@code combos}: result per combination of the slot selection
 * @param combos  slots do not change on their own but by click; the result follows the selection
 *                (index = mixed base over all slots with several options, first slot first)
 */
public record RecipeView(String type, int width, int height, List<List<ItemRef>> slots, ItemRef result, ItemRef station,
                         RecipeKind kind, List<String> notes, List<Row> rows, List<ItemRef> results, boolean combos) {

    /** Row of a list. */
    public record Row(ItemRef icon, String label) {}

    public RecipeView(String type, int width, int height, List<List<ItemRef>> slots, ItemRef result, ItemRef station,
                      RecipeKind kind) {
        this(type, width, height, slots, result, station, kind, List.of(), List.of(), List.of(), false);
    }

    public RecipeView(String type, int width, int height, List<List<ItemRef>> slots, ItemRef result, ItemRef station) {
        this(type, width, height, slots, result, station, RecipeKind.OTHER);
    }

    /** Grid → result with text lines (e.g. trade). */
    public static RecipeView withNotes(String type, List<List<ItemRef>> slots, ItemRef result, ItemRef station, List<String> notes) {
        return new RecipeView(type, Math.max(1, slots.size()), 1, slots, result, station, RecipeKind.OTHER, notes, List.of(), List.of(), false);
    }

    /** Plain list (e.g. "Dropped by", "Spawns in"). */
    public static RecipeView list(String type, ItemRef station, List<String> notes, List<Row> rows) {
        return new RecipeView(type, 0, 0, List.of(), ItemRef.EMPTY, station, RecipeKind.OTHER, notes, rows, List.of(), false);
    }

    public boolean isList() { return width == 0; }
}
