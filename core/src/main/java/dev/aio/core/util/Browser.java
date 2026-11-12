package dev.aio.core.util;

import java.util.Locale;

/** Opens a web address in the system browser (only https links). */
public final class Browser {
    private Browser() {}

    public static void open(String url) {
        if (url == null || !url.startsWith("https://")) return;
        try {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            String[] cmd = os.contains("win") ? new String[]{"rundll32", "url.dll,FileProtocolHandler", url}
                    : os.contains("mac") ? new String[]{"open", url} : new String[]{"xdg-open", url};
            new ProcessBuilder(cmd).start();
        } catch (Exception e) {
            Log.warn("[aio] Could not open the browser: " + e.getClass().getSimpleName());
        }
    }
}
