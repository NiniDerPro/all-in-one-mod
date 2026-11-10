package dev.aio.fabric;

import com.google.gson.Gson;

import dev.aio.core.AioCore;
import dev.aio.fabric.compat.HookStatus;
import dev.aio.fabric.compat.ModConflicts;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

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
 * Anonyme Nutzungsstatistik an den eigenen Statistik-Server. Sendet nur, wenn der Nutzer "Send
 * anonymous usage statistics" eingeschaltet hat (standardmäßig aus, Opt-in über den Hinweis beim
 * ersten Start) und {@link #ENDPOINT} gesetzt ist. Details und die komplette Feldliste:
 * {@code docs/TELEMETRY.md}. Gesendet werden nur technische Zahlen, nie Name, UUID, IP, Server,
 * Koordinaten oder Chat.
 */
final class Telemetry {
    private Telemetry() {}

    /** Adresse des Statistik-Servers (HTTPS). Leer = es wird nichts gesendet. */
    private static final String ENDPOINT = "";

    private static final long FIRST_DELAY_S = 90;
    private static final long PERIOD_MIN = 10;

    private static final Gson GSON = new Gson();
    private static AioCore core;
    private static ScheduledExecutorService scheduler;
    private static HttpClient http;

    // Leistung dieser Sitzung (nur Zähler, keine Einzelwerte)
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

    /** Jeden Client-Tick: alle 5 Sekunden die FPS in die Sitzungs-Zähler aufnehmen. */
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
            // Statistik darf das Spiel nie stören: Fehler werden still verworfen
        }
    }

    /** Alles, was gesendet wird. Nur technische, anonyme Werte. */
    static Map<String, Object> payload() {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("schema", 1);
        p.put("installId", core.stats().installId());

        // Versionen
        Map<String, Object> ver = new LinkedHashMap<>();
        ver.put("mod", FabricLoader.getInstance().getModContainer("aio")
                .map(ModContainer::getMetadata).map(m -> m.getVersion().getFriendlyString()).orElse("?"));
        ver.put("minecraft", FabricLoader.getInstance().getModContainer("minecraft")
                .map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("?"));
        ver.put("loader", FabricLoader.getInstance().getModContainer("fabricloader")
                .map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("?"));
        ver.put("java", System.getProperty("java.specification.version", "?"));
        ver.put("os", os());
        p.put("versions", ver);

        // Nutzung
        Map<String, Object> use = new LinkedHashMap<>();
        use.put("launches", core.stats().launches());
        use.put("playHours", bucketHours(core.stats().totalPlayMillis()));
        use.put("sessionMinutes", (System.currentTimeMillis() - startedMs) / 60_000);
        p.put("usage", use);

        // Module: aktive plus grobe Nutzungsdauer je Modul
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

        // Kompatibilität und Fehler
        Map<String, Object> compat = new LinkedHashMap<>();
        compat.put("deferredTo", ModConflicts.installed()); // Modul -> Name der Mod, die Vorrang hat
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

    /** Dauer in grobe Gruppen, damit keine exakten Zeiten übertragen werden. */
    private static String bucketHours(long ms) {
        double h = ms / 3_600_000.0;
        if (h < 1) return "<1";
        if (h < 10) return "1-10";
        if (h < 50) return "10-50";
        if (h < 200) return "50-200";
        return "200+";
    }
}
