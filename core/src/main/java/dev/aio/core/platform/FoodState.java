package dev.aio.core.platform;

/**
 * Hunger-Stand des Spielers für die Essens-Info.
 *
 * @param exhaustion   Erschöpfung 0..4, NaN wenn unbekannt (Server schicken sie nicht)
 * @param heldNutrition Nährwert des Essens in der Hand, 0 = kein Essen
 * @param heldCanEat   ob man es gerade essen könnte (satt? immer essbar?)
 * @param visible      ob die Hungerleiste zu sehen ist (nicht im Kreativmodus, nicht auf Reittieren)
 */
public record FoodState(int food, float saturation, float exhaustion, float health, float maxHealth, float absorption,
                        int heldNutrition, float heldSaturation, boolean heldCanEat, boolean visible) {
    public static final FoodState NONE = new FoodState(0, 0, Float.NaN, 0, 20, 0, 0, 0, false, false);
}
