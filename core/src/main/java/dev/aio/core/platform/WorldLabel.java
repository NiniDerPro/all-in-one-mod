package dev.aio.core.platform;

import java.util.List;

/** Text above an object in the world (e.g. trade offers above a villager). */
public record WorldLabel(double x, double y, double z, double distance, List<Line> lines) {
    /** One line with colour; {@code book} = enchanted book offer. */
    public record Line(String text, int color, boolean book) {}
}
