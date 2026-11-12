package dev.aio.core.util;

/** Minimal logging facade so that the core needs no logging library. The adapter sets {@link #sink}. */
public final class Log {
    public interface Sink {
        void log(String level, String message, Throwable error);
    }

    public static volatile Sink sink = (level, message, error) -> {
        System.err.println("[AIO/" + level + "] " + message);
        if (error != null) error.printStackTrace();
    };

    private Log() {}

    public static void info(String msg) { sink.log("INFO", msg, null); }
    public static void warn(String msg) { sink.log("WARN", msg, null); }
    public static void error(String msg, Throwable t) { sink.log("ERROR", msg, t); }
}
