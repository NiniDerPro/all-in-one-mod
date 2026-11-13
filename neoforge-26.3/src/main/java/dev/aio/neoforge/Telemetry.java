package dev.aio.neoforge;

import com.google.gson.Gson;

import dev.aio.core.AioCore;
import dev.aio.neoforge.compat.HookStatus;
import dev.aio.neoforge.compat.ModConflicts;

import dev.aio.neoforge.shim.Loader;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Anonymous usage statistics to the project's own statistics server. Only sends if the user has switched "Send
 * anonymous usage statistics" on (off by default, opt-in via the notice on the first
 * start) and {@link #ENDPOINT} is set. Details and the complete field list:
 * {@code docs/TELEMETRY.md}. Only technical numbers are sent, never name, UUID, IP, server,
 * coordinates or chat.
 */
final class Telemetry {
    private Telemetry() {}

    /**
     * Address of the statistics server (HTTPS). Deliberately not in the source code: the build writes it
     * from the environment variable {@code AIO_STATS_URL} into the file {@code aio_stats_url.txt}. Without
     * this variable (e.g. in own builds) it is empty and nothing is sent.
     */
    private static final String ENDPOINT = loadEndpoint();

    private static String loadEndpoint() {
        try (var in = Telemetry.class.getResourceAsStream("/aio_stats_url.txt")) {
            return in == null ? "" : new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static final long FIRST_DELAY_S = 90;
    private static final long PERIOD_MIN = 10;

    private static final Gson GSON = new Gson();
    private static AioCore core;
    private static ScheduledExecutorService scheduler;
    private static HttpClient http;

    // Performance of this session (counters only, no single values)
    private static long fpsSum, fpsCount;
    private static int fpsMin = Integer.MAX_VALUE;
    private static int tickCounter;
    private static final long startedMs = System.currentTimeMillis();

    static void init(AioCore aioCore) {
        core = aioCore;
        if (ENDPOINT.isEmpty() || !ENDPOINT.startsWith("https://")) return;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "aio-stats");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleWithFixedDelay(Telemetry::sendSafely, FIRST_DELAY_S, PERIOD_MIN * 60, TimeUnit.SECONDS);
    }

    /** Every client tick: take the FPS into the session counters every 5 seconds. */
    static void tick(int fps) {
        if (++tickCounter % 100 != 0 || fps <= 0) return;
        fpsSum += fps;
        fpsCount++;
        if (fps < fpsMin) fpsMin = fps;
    }

    private static void sendSafely() {
        try {
            if (core == null || !core.clientSettings().telemetry.on()) return;
            if (http == null) http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
            HttpRequest req = HttpRequest.newBuilder(URI.create(ENDPOINT))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload())))
                    .build();
            http.send(req, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
            // Statistics must never disturb the game: errors are silently discarded
        }
    }

    /** Everything that is sent. Only technical, anonymous values. */
    static Map<String, Object> payload() {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("schema", 1);
        p.put("installId", core.stats().installId());

        // Versionen
        Map<String, Object> ver = new LinkedHashMap<>();
        ver.put("mod", Loader.modVersion("aio").orElse("?"));
        ver.put("minecraft", Loader.modVersion("minecraft").orElse("?"));
        ver.put("loader", "neoforge " + Loader.modVersion("neoforge").orElse("?"));
        ver.put("java", System.getProperty("java.specification.version", "?"));
        ver.put("os", os());
        p.put("versions", ver);

        // Nutzung
        Map<String, Object> use = new LinkedHashMap<>();
        use.put("launches", core.stats().launches());
        use.put("playHours", bucketHours(core.stats().totalPlayMillis()));
        use.put("sessionMinutes", (System.currentTimeMillis() - startedMs) / 60_000);
        p.put("usage", use);

        // Modules: active ones plus rough usage time per module
        List<String> active = new ArrayList<>();
        core.modules().all().forEach(m -> {
            if (m.canToggle() && m.isActive()) active.add(m.id());
        });
        Map<String, String> durations = new LinkedHashMap<>();
        core.stats().moduleMillis().forEach((id, ms) -> durations.put(id, bucketHours(ms)));
        Map<String, Object> modules = new LinkedHashMap<>();
        modules.put("active", active);
        modules.put("activeCount", active.size());
        modules.put("peakActive", core.stats().peakActiveModules());
        modules.put("hours", durations);
        p.put("modules", modules);

        // Compatibility and errors
        Map<String, Object> compat = new LinkedHashMap<>();
        compat.put("deferredTo", ModConflicts.installed()); // module -> name of the mod that takes priority
        compat.put("brokenHooks", new ArrayList<>(HookStatus.broken().keySet()));
        compat.put("missingMixins", HookStatus.missing());
        compat.put("compatMode", core.clientSettings().compatMode.get().name());
        p.put("compat", compat);

        // Leistung (Durchschnitt/Minimum dieser Sitzung)
        Map<String, Object> perf = new LinkedHashMap<>();
        perf.put("avgFps", fpsCount == 0 ? 0 : Math.round((float) fpsSum / fpsCount));
        perf.put("minFps", fpsCount == 0 ? 0 : fpsMin);
        perf.put("samples", fpsCount);
        p.put("performance", perf);
        return p;
    }

    private static String os() {
        String n = System.getProperty("os.name", "").toLowerCase();
        if (n.contains("win")) return "windows";
        if (n.contains("mac") || n.contains("darwin")) return "macos";
        if (n.contains("linux")) return "linux";
        return "other";
    }

    /** Duration in rough groups so that no exact times are transmitted. */
    private static String bucketHours(long ms) {
        double h = ms / 3_600_000.0;
        if (h < 1) return "<1";
        if (h < 10) return "1-10";
        if (h < 50) return "10-50";
        if (h < 200) return "50-200";
        return "200+";
    }
}
