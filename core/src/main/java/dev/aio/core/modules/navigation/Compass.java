package dev.aio.core.modules.navigation;

/** Kompass-Mathematik. Kompasskurs: 0 = Norden, 90 = Osten, 180 = Süden, 270 = Westen. */
public final class Compass {
    private static final String[] LABELS = {"N", "NE", "O", "SE", "S", "SW", "W", "NW"};

    private Compass() {}

    /** MC-Yaw (0 = Süden, 90 = Westen) zu Kompasskurs. */
    public static float heading(float yaw) {
        return wrap(yaw + 180f);
    }

    /** Kurs vom Spieler zu einem Punkt (dx = Ziel.x - x, dz = Ziel.z - z). */
    public static float bearing(double dx, double dz) {
        return wrap((float) Math.toDegrees(Math.atan2(dx, -dz)));
    }

    /** Winkeldifferenz -180..180 (positiv = Ziel liegt rechts). */
    public static float delta(float from, float to) {
        float d = wrap(to - from);
        return d > 180 ? d - 360 : d;
    }

    public static float wrap(float deg) {
        float d = deg % 360f;
        return d < 0 ? d + 360 : d;
    }

    /** Beschriftung für Vielfache von 45°, sonst null. */
    public static String label(int degrees) {
        int d = Math.floorMod(degrees, 360);
        return d % 45 == 0 ? LABELS[d / 45] : null;
    }
}
