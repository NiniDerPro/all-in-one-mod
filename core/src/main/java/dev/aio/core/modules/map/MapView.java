package dev.aio.core.modules.map;

/** Ausschnitt der Karte auf dem Bildschirm und die Umrechnung zwischen Welt und Pixeln. */
public record MapView(float x0, float y0, float w, float h, double cx, double cz, float ppb, int refH, boolean diorama) {
    /** Schräger Look: wie stark die Zeilen gestaucht sind und wie viele Pixel ein Höhenblock hebt. */
    static final float TILT = 0.6f;

    float lift() { return ppb * 0.8f; }

    public float sx(double x) { return x0 + w / 2f + (float) (x - cx) * ppb; }

    public float sy(double z, int height) {
        float y = y0 + h / 2f + (float) (z - cz) * ppb * (diorama ? TILT : 1f);
        return diorama ? y - (height - refH) * lift() : y;
    }

    /** Weltpunkt unter einem Bildschirmpunkt (im schrägen Look über die Höhe iteriert). */
    public int[] worldAt(MapStore s, double mx, double my) {
        double x = cx + (mx - x0 - w / 2f) / ppb;
        double z = cz + (my - y0 - h / 2f) / (ppb * (diorama ? TILT : 1f));
        if (diorama) {
            for (int i = 0; i < 3; i++) {
                int hh = s.height((int) Math.floor(x), (int) Math.floor(z));
                if (s.rgb((int) Math.floor(x), (int) Math.floor(z)) == 0) hh = refH;
                z = cz + (my - y0 - h / 2f + (hh - refH) * lift()) / (ppb * TILT);
            }
        }
        return new int[]{(int) Math.floor(x), (int) Math.floor(z)};
    }
}
