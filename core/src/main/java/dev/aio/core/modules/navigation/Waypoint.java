package dev.aio.core.modules.navigation;

/**
 * Ein Wegpunkt.
 *
 * @param world     world key (server or singleplayer world)
 * @param dimension z. B. "overworld"
 * @param death     set automatically on death; disappears when you arrive
 */
public record Waypoint(String name, int x, int y, int z, String world, String dimension, int color, boolean visible, boolean death) {
    public Waypoint withVisible(boolean v) { return new Waypoint(name, x, y, z, world, dimension, color, v, death); }

    public Waypoint withName(String n) { return new Waypoint(n, x, y, z, world, dimension, color, visible, death); }

    public Waypoint withColor(int c) { return new Waypoint(name, x, y, z, world, dimension, c, visible, death); }

    public double distanceTo(double px, double py, double pz) {
        double dx = x + 0.5 - px, dy = y + 0.5 - py, dz = z + 0.5 - pz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
