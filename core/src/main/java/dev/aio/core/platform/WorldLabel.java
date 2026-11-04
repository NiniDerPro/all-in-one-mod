package dev.aio.core.platform;

import java.util.List;

/** Text über einem Objekt in der Welt (z. B. Handelsangebote über einem Dorfbewohner). */
public record WorldLabel(double x, double y, double z, double distance, List<Line> lines) {
    /** Eine Zeile mit Farbe; {@code book} = Verzauberungsbuch-Angebot. */
    public record Line(String text, int color, boolean book) {}
}
