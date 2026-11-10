package dev.aio.fabric;

import dev.aio.core.AioCore;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Anonyme Online-Statistik über bStats – nur aggregierte Zahlen, keine Namen, keine IPs bei uns
 * (bStats verwirft die IP sofort). Sendet nur, wenn der Nutzer "Send anonymous usage statistics"
 * anlässt (standardmäßig aus, Opt-in per Hinweis beim ersten Start) und eine gültige bStats-Service-ID gesetzt ist.
 *
 * <p>bStats wird über Reflection angesprochen, damit die Mod auch ohne die bStats-Bibliothek baut und
 * läuft (dann passiert hier schlicht nichts, die lokale Statistik läuft weiter). Zum Scharfschalten:
 * bStats ins Jar einbinden (siehe {@code docs/TELEMETRY.md}) und {@link #SERVICE_ID} auf die eigene
 * bStats-Service-ID setzen.
 */
final class Telemetry {
    private Telemetry() {}

    /**
     * Eigene bStats-Service-ID (auf bStats.org beim Anlegen des Mod-Eintrags vergeben). 0 = aus:
     * solange hier 0 steht, wird nichts gesendet, auch wenn der Schalter an ist.
     */
    private static final int SERVICE_ID = 0;

    private static AioCore core;
    private static Object metrics;
    private static boolean bstatsMissingLogged;

    static void init(AioCore aioCore) {
        core = aioCore;
        core.clientSettings().telemetry.onChange(on -> {
            if (on) start();
            else stop();
        });
        if (core.clientSettings().telemetry.on()) start();
    }

    private static synchronized void start() {
        if (metrics != null || SERVICE_ID <= 0) return;
        ModContainer mod = FabricLoader.getInstance().getModContainer("aio").orElse(null);
        if (mod == null) return;
        try {
            Class<?> metricsCls = Class.forName("org.bstats.fabric.Metrics");
            Constructor<?> ctor = metricsCls.getConstructor(ModContainer.class, int.class);
            Object m = ctor.newInstance(mod, SERVICE_ID);
            Class<?> chartCls = Class.forName("org.bstats.charts.CustomChart");
            Method add = metricsCls.getMethod("addCustomChart", chartCls);

            add.invoke(m, intChart("active_modules", () -> core.stats().activeModuleCount()));
            add.invoke(m, intChart("peak_active_modules", () -> core.stats().peakActiveModules()));
            add.invoke(m, pie("most_used_module", Telemetry::mostUsedModule));
            add.invoke(m, advancedPie("modules_in_use", Telemetry::modulesInUse));

            metrics = m;
            AioFabricClient.LOGGER.info("[aio] Anonymous statistics (bStats) started");
        } catch (ClassNotFoundException e) {
            if (!bstatsMissingLogged) {
                bstatsMissingLogged = true;
                AioFabricClient.LOGGER.info("[aio] bStats not bundled; only local statistics are kept");
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            AioFabricClient.LOGGER.warn("[aio] Could not start anonymous statistics", e);
        }
    }

    private static synchronized void stop() {
        if (metrics == null) return;
        try {
            metrics.getClass().getMethod("shutdown").invoke(metrics);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // kein shutdown verfügbar: nächster Neustart sendet dann einfach nicht mehr
        }
        metrics = null;
    }

    /** Am längsten genutztes Modul, für die "meistgenutzt"-Grafik. */
    private static String mostUsedModule() {
        String best = "none";
        long bestMs = 0;
        for (Map.Entry<String, Long> e : core.stats().moduleMillis().entrySet()) {
            if (e.getValue() > bestMs) {
                bestMs = e.getValue();
                best = e.getKey();
            }
        }
        return best;
    }

    /** Welche Module gerade aktiv sind (je 1), damit bStats über alle Nutzer zählt, was beliebt ist. */
    private static Map<String, Integer> modulesInUse() {
        Map<String, Integer> out = new LinkedHashMap<>();
        core.modules().all().forEach(m -> {
            if (m.canToggle() && m.isActive()) out.put(m.id(), 1);
        });
        return out;
    }

    private static Object intChart(String id, Callable<Integer> value) throws ReflectiveOperationException {
        return Class.forName("org.bstats.charts.SingleLineChart")
                .getConstructor(String.class, Callable.class).newInstance(id, value);
    }

    private static Object pie(String id, Callable<String> value) throws ReflectiveOperationException {
        return Class.forName("org.bstats.charts.SimplePie")
                .getConstructor(String.class, Callable.class).newInstance(id, value);
    }

    private static Object advancedPie(String id, Callable<Map<String, Integer>> value) throws ReflectiveOperationException {
        return Class.forName("org.bstats.charts.AdvancedPie")
                .getConstructor(String.class, Callable.class).newInstance(id, value);
    }
}
