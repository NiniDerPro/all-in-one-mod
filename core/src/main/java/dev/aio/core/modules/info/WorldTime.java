package dev.aio.core.modules.info;

/** Conversion of the Minecraft time of day. Tick 0 = 6:00, a day has 24000 ticks. */
public final class WorldTime {
    private WorldTime() {}

    public static long day(long dayTime) { return Math.floorDiv(dayTime, 24000L) + 1; }

    public static int hours(long dayTime) {
        long t = Math.floorMod(dayTime, 24000L);
        return (int) ((t / 1000 + 6) % 24);
    }

    public static int minutes(long dayTime) {
        long t = Math.floorMod(dayTime, 24000L);
        return (int) ((t % 1000) * 60 / 1000);
    }

    /** Night: monsters can spawn (about 19:12 to 4:48). */
    public static boolean isNight(long dayTime) {
        long t = Math.floorMod(dayTime, 24000L);
        return t >= 13188 && t <= 22812;
    }

    public static String format(long dayTime, boolean twelveHour) {
        int h = hours(dayTime), m = minutes(dayTime);
        if (!twelveHour) return String.format("%02d:%02d", h, m);
        int h12 = h % 12 == 0 ? 12 : h % 12;
        return String.format("%d:%02d %s", h12, m, h < 12 ? "AM" : "PM");
    }
}
