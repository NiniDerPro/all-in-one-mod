package dev.aio.core.modules.navigation;

/** Compass maths. Compass bearing: 0 = north, 90 = east, 180 = south, 270 = west. */
public final class Compass {
    private static final String[] LABELS = {"N", "NE", "O", "SE", "S", "SW", "W", "NW"};

    private Compass() {}

    /** MC yaw (0 = south, 90 = west) to compass bearing. */
    public static float heading(float yaw) {
        return wrap(yaw + 180f);
    }

    /** Kurs vom Spieler zu einem Punkt (dx = Ziel.x - x, dz = Ziel.z - z). */
    public static float bearing(double dx, double dz) {
        return wrap((float) Math.toDegrees(Math.atan2(dx, -dz)));
    }

    /** Angle difference -180..180 (positive = target is to the right). */
    public static float delta(float from, float to) {
        float d = wrap(to - from);
        return d > 180 ? d - 360 : d;
    }

    public static float wrap(float deg) {
        float d = deg % 360f;
        return d < 0 ? d + 360 : d;
    }

    /** Label for multiples of 45°, otherwise null. */
    public static String label(int degrees) {
        int d = Math.floorMod(degrees, 360);
        return d % 45 == 0 ? LABELS[d / 45] : null;
    }
}
