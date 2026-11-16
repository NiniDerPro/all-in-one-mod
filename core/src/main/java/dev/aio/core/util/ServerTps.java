package dev.aio.core.util;

/**
 * Estimates the server TPS from the time updates the server sends every 20 ticks
 * (the adapter calls {@link #onTimePacket()} on the game thread when one arrives).
 */
public final class ServerTps {
    private static final int SAMPLES = 6;
    private static final long[] intervals = new long[SAMPLES];
    private static int count, next;
    private static long last;

    private ServerTps() {}

    public static synchronized void onTimePacket() {
        long now = System.nanoTime();
        if (last != 0) {
            long dt = now - last;
            if (dt > 50_000_000L && dt < 60_000_000_000L) { // ignore duplicates and very long pauses
                intervals[next] = dt;
                next = (next + 1) % SAMPLES;
                count = Math.min(SAMPLES, count + 1);
            }
        }
        last = now;
    }

    public static synchronized void reset() {
        count = next = 0;
        last = 0;
    }

    /** Estimated TPS (max 20), or NaN while too few updates arrived. */
    public static synchronized double tps() {
        if (count < 2) return Double.NaN;
        // no update for a long time: the server is stuck
        long since = System.nanoTime() - last;
        long sum = 0;
        for (int i = 0; i < count; i++) sum += intervals[i];
        double avg = sum / (double) count;
        if (since > avg * 2 && since > 3_000_000_000L) avg = since;
        return Math.min(20.0, 20.0 * 1_000_000_000.0 / avg);
    }

    /** Seconds since the last time update from the server (server lag), NaN if none yet. */
    public static synchronized double secondsSinceUpdate() {
        return last == 0 ? Double.NaN : (System.nanoTime() - last) / 1e9;
    }
}
