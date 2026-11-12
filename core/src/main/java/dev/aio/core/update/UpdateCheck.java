package dev.aio.core.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.aio.core.AioCore;
import dev.aio.core.util.Log;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Looks once per start whether a newer release exists for the running Minecraft version (GitHub releases,
 * tags like {@code v0.4.2-mc26.3}). It only ever shows a notice with a link; nothing is downloaded or installed.
 * Can be switched off in the client settings. Any failure is silent.
 */
public final class UpdateCheck {
    private UpdateCheck() {}

    public record Release(String version, String url) {}

    private static final String API = "https://api.github.com/repos/NiniDerPro/all-in-one-mod/releases?per_page=60";
    private static final Pattern TAG = Pattern.compile("^v(\\d+(?:\\.\\d+)*)-mc(.+)$");
    private static final Pattern RUNNING = Pattern.compile("^(\\d+(?:\\.\\d+)*)\\+mc(.+)$");

    private static volatile Release available;
    private static volatile boolean dismissed;

    /** The newer release found, or null. */
    public static Release available() { return available; }

    /** For the developer self-test: pretend that a release exists. */
    public static void preview(Release r) { available = r; }

    /** "Later": hide the notice until the next start. */
    public static void dismissForSession() { dismissed = true; }

    public static boolean pending(AioCore core) {
        Release r = available;
        var s = core.clientSettings();
        return r != null && !dismissed && s.updateCheck.on() && s.telemetryNoticeSeen.on()
                && !r.version().equals(s.skippedUpdate.get());
    }

    /** Starts the background check. {@code runningVersion} is the jar version, e.g. {@code 0.4.2+mc26.3}. */
    public static void start(AioCore core, String runningVersion) {
        if (!core.clientSettings().updateCheck.on()) return;
        Thread t = new Thread(() -> {
            try {
                HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
                HttpRequest req = HttpRequest.newBuilder(URI.create(API)).timeout(Duration.ofSeconds(8))
                        .header("Accept", "application/vnd.github+json").header("User-Agent", "AIO-ClientSide").build();
                HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() == 200) available = pick(res.body(), runningVersion);
            } catch (Exception e) {
                Log.info("[aio] Update check skipped: " + e.getClass().getSimpleName());
            }
        }, "aio-update-check");
        t.setDaemon(true);
        t.start();
    }

    /** The newest release for the same Minecraft version that is newer than the running one, or null. */
    public static Release pick(String releasesJson, String runningVersion) {
        Matcher run = RUNNING.matcher(runningVersion == null ? "" : runningVersion);
        if (!run.matches()) return null;
        String current = run.group(1), mc = run.group(2);
        JsonElement root = JsonParser.parseString(releasesJson);
        if (!root.isJsonArray()) return null;
        Release best = null;
        JsonArray list = root.getAsJsonArray();
        for (JsonElement e : list) {
            if (!e.isJsonObject()) continue;
            JsonObject o = e.getAsJsonObject();
            if (bool(o, "draft")) continue;
            Matcher m = TAG.matcher(str(o, "tag_name"));
            if (!m.matches() || !m.group(2).equals(mc)) continue;
            String v = m.group(1);
            if (compare(v, current) <= 0) continue;
            if (best == null || compare(v, best.version()) > 0) best = new Release(v, str(o, "html_url"));
        }
        return best;
    }

    /** Compares dotted numbers (0.4.10 > 0.4.2). */
    public static int compare(String a, String b) {
        String[] x = a.split("\\."), y = b.split("\\.");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int p = i < x.length ? num(x[i]) : 0, q = i < y.length ? num(y[i]) : 0;
            if (p != q) return Integer.compare(p, q);
        }
        return 0;
    }

    private static int num(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }

    private static String str(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : "";
    }

    private static boolean bool(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return e != null && e.isJsonPrimitive() && e.getAsBoolean();
    }
}
