package dev.aio.core.platform;

import java.util.List;

/**
 * Ein Eintrag in der Rezeptansicht: ein Rezept, ein Handel, eine Beute-Liste, ...
 *
 * @param type    Anzeigename der Art, z. B. "Werkbank", "Handel", "Beute"
 * @param width   Raster-Breite (0 = kein Raster, nur Liste)
 * @param slots   pro Eingabe-Slot die möglichen Items (leer = leerer Slot); mehrere werden durchgewechselt
 * @param result  Ergebnis (EMPTY = keins)
 * @param kind    Art für das Übertragen ins Inventar ("+")
 * @param notes   Textzeilen unter dem Raster (Stufe, Chance, ...)
 * @param rows    Liste aus Symbol und Text (z. B. Beute mit Chance), wird bei Bedarf geblättert
 * @param results bei {@code combos}: Ergebnis je Kombination der Slot-Auswahl
 * @param combos  Slots wechseln nicht von selbst, sondern per Klick; das Ergebnis folgt der Auswahl
 *                (Index = gemischte Basis über alle Slots mit mehreren Möglichkeiten, erster Slot zuerst)
 */
public record RecipeView(String type, int width, int height, List<List<ItemRef>> slots, ItemRef result, ItemRef station,
                         RecipeKind kind, List<String> notes, List<Row> rows, List<ItemRef> results, boolean combos) {

    /** Zeile einer Liste. */
    public record Row(ItemRef icon, String label) {}

    public RecipeView(String type, int width, int height, List<List<ItemRef>> slots, ItemRef result, ItemRef station,
                      RecipeKind kind) {
        this(type, width, height, slots, result, station, kind, List.of(), List.of(), List.of(), false);
    }

    public RecipeView(String type, int width, int height, List<List<ItemRef>> slots, ItemRef result, ItemRef station) {
        this(type, width, height, slots, result, station, RecipeKind.OTHER);
    }

    /** Raster → Ergebnis mit Textzeilen (z. B. Handel). */
    public static RecipeView withNotes(String type, List<List<ItemRef>> slots, ItemRef result, ItemRef station, List<String> notes) {
        return new RecipeView(type, Math.max(1, slots.size()), 1, slots, result, station, RecipeKind.OTHER, notes, List.of(), List.of(), false);
    }

    /** Reine Liste (z. B. "Droppt von", "Spawnt in"). */
    public static RecipeView list(String type, ItemRef station, List<String> notes, List<Row> rows) {
        return new RecipeView(type, 0, 0, List.of(), ItemRef.EMPTY, station, RecipeKind.OTHER, notes, rows, List.of(), false);
    }

    public boolean isList() { return width == 0; }
}
