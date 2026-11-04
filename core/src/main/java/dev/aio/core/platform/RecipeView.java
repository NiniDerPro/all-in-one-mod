package dev.aio.core.platform;

import java.util.List;

/**
 * Ein Rezept zur Anzeige in der Item-Liste.
 *
 * @param type   Art, z. B. "Werkbank", "Ofen"
 * @param width  Raster-Breite (1 bei Ofen-Rezepten)
 * @param slots  pro Eingabe-Slot die möglichen Items (leer = leerer Slot); mehrere werden durchgewechselt
 * @param result Ergebnis
 */
public record RecipeView(String type, int width, int height, List<List<ItemRef>> slots, ItemRef result, ItemRef station) {}
