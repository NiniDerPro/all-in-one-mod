package dev.aio.core.stats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import dev.aio.core.config.ConfigManager;
import dev.aio.core.event.Events;
import dev.aio.core.module.Module;
import dev.aio.core.module.ModuleRegistry;
import dev.aio.core.util.Log;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * Private Nutzungsstatistik, die nur lokal in {@code config/aio/stats.json} liegt. Keine Anzeige im
 * Spiel, nichts wird von hier verschickt. Zählt die Spielzeit mit der Mod, wie lange und wie oft
 * jedes Modul aktiv war, die Zahl der Starts, den höchsten gleichzeitigen Modulstand und bietet
 * {@link #add} für beliebige weitere Zähler (für später).
 *
 * <p>Die anonyme Online-Statistik (bStats) liest diese Werte nur aus; das Senden passiert
 * ausschließlich im Fabric-Teil und nur, wenn der Nutzer es eingeschaltet lässt.
 */
public final class Stats {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE = "stats.json";
    private static final long SAVE_INTERVAL_MS = 60_000;
    /** Große Sprünge (Pause, Standby, Fenster lange weg) nicht als Spielzeit zählen. */
    private static final long MAX_DELTA_MS = 10_000;

    private final ConfigManager config;
    private final ModuleRegistry modules;
    private final LongSupplier clock;

    private Data data = new Data();
    private long lastTickMs;
    private long lastSaveMs;
    private Set<String> activeBefore = Set.of();

    public Stats(ConfigManager config, ModuleRegistry modules, LongSupplier clock) {
        this.config = config;
        this.modules = modules;
        this.clock = clock;
    }

    /** Beim Start: laden, Installations-ID sichern, als neuen Start zählen. */
    public void start() {
        load();
        if (data.installId == null || data.installId.isBlank()) data.installId = UUID.randomUUID().toString();
        if (data.firstStartMillis == 0) data.firstStartMillis = clock.getAsLong();
        data.launches++;
        long now = clock.getAsLong();
        lastTickMs = now;
        lastSaveMs = now;
        activeBefore = activeModuleIds();
        save();
    }

    /** Jeden Client-Tick aufrufen: Spielzeit und Modulzeiten fortschreiben. */
    public void tick() {
        long now = clock.getAsLong();
        long delta = now - lastTickMs;
        lastTickMs = now;
        if (delta <= 0 || delta > MAX_DELTA_MS) delta = 0;

        Set<String> active = activeModuleIds();
        if (delta > 0) {
            data.totalPlayMillis += delta;
            for (String id : active) data.moduleMillis.merge(id, delta, Long::sum);
        }
        if (active.size() > data.peakActiveModules) data.peakActiveModules = active.size();
        for (String id : active) if (!activeBefore.contains(id)) data.moduleActivations.merge(id, 1L, Long::sum);
        activeBefore = active;

        if (now - lastSaveMs >= SAVE_INTERVAL_MS) save();
    }

    /** Beliebigen Zähler erhöhen (z. B. "menuOpens", "waypointsCreated"); für künftige Statistiken. */
    public void add(String counter, long amount) {
        data.counters.merge(counter, amount, Long::sum);
    }

    public void increment(String counter) { add(counter, 1); }

    /** Beim Beenden sichern. */
    public void flush() { save(); }

    public String installId() { return data.installId; }

    public long totalPlayMillis() { return data.totalPlayMillis; }

    public int launches() { return data.launches; }

    public int peakActiveModules() { return data.peakActiveModules; }

    /** Momentan aktive, abschaltbare Module. */
    public int activeModuleCount() { return activeModuleIds().size(); }

    /** Nach Nutzungszeit absteigend sortierte Modul-IDs (für "meist genutzt"). */
    public Map<String, Long> moduleMillis() { return new TreeMap<>(data.moduleMillis); }

    private Set<String> activeModuleIds() {
        Set<String> out = new TreeSet<>();
        for (Module m : modules.all()) if (m.canToggle() && m.isActive()) out.add(m.id());
        return out;
    }

    private void load() {
        String json = config.readText(FILE);
        if (json == null) return;
        try {
            Data d = GSON.fromJson(json, Data.class);
            if (d != null) {
                data = d;
                data.fillNulls();
            }
        } catch (JsonSyntaxException e) {
            Log.error("stats.json ist defekt, beginne neu", e);
        }
    }

    private void save() {
        lastSaveMs = clock.getAsLong();
        config.writeText(FILE, GSON.toJson(data));
    }

    /** Auf Platte gespeicherter Stand. Neue Felder einfach ergänzen (alte Dateien füllen Standard). */
    private static final class Data {
        String installId;
        long firstStartMillis;
        int launches;
        long totalPlayMillis;
        int peakActiveModules;
        Map<String, Long> moduleMillis = new TreeMap<>();
        Map<String, Long> moduleActivations = new TreeMap<>();
        Map<String, Long> counters = new TreeMap<>();

        void fillNulls() {
            if (moduleMillis == null) moduleMillis = new TreeMap<>();
            if (moduleActivations == null) moduleActivations = new TreeMap<>();
            if (counters == null) counters = new TreeMap<>();
        }
    }
}
