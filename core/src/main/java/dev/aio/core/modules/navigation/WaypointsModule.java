package dev.aio.core.modules.navigation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aio.core.event.Events;
import dev.aio.core.hud.HudElement;
import dev.aio.core.module.Category;
import dev.aio.core.platform.CameraView;
import dev.aio.core.platform.Renderer;
import dev.aio.core.platform.Vec3;
import dev.aio.core.setting.ActionSetting;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.KeybindSetting;
import dev.aio.core.setting.NumberSetting;
import dev.aio.core.setting.StringSetting;
import dev.aio.core.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Wegpunkte pro Welt und Dimension: Marker mit Name und Entfernung im Bild, am Bildrand,
 * wenn sie außerhalb liegen. Verwaltung über einen eigenen Screen.
 */
public final class WaypointsModule extends HudElement {
    public static final int[] PALETTE = {0xFF5555, 0xFFAA00, 0xFFFF55, 0x55FF55, 0x55FFFF, 0x5599FF, 0xAA55FF, 0xFF55FF, 0xFFFFFF};

    private final StringSetting data;
    private final KeybindSetting addKey;
    private final KeybindSetting manageKey;
    private final BooleanSetting showDistance;
    private final BooleanSetting edgeMarkers;
    private final NumberSetting maxDistance;
    private final List<Waypoint> waypoints = new ArrayList<>();
    private boolean loaded;

    public WaypointsModule() {
        super("waypoints", "Waypoints", "Waypoints with distance on screen and on the compass", Category.QOL, false, 0, 0);
        // nur noch zum Übernehmen alter Daten (früher im Profil gespeichert)
        data = add(new StringSetting("data", "Data (old)", "[]", 1_000_000));
        data.hidden();
        posX.hidden();
        posY.hidden();
        addKey = add(new KeybindSetting("addKey", "Add waypoint", "key.keyboard.b"));
        manageKey = add(new KeybindSetting("manageKey", "Manage waypoints", "key.keyboard.n"));
        add(new ActionSetting("manage", "Manage waypoints", () -> core.platform().openScreen(new WaypointScreen(core, this))));
        showDistance = add(new BooleanSetting("distance", "Show distance", true));
        edgeMarkers = add(new BooleanSetting("edge", "Show at screen edge", true));
        maxDistance = add(new NumberSetting("maxDistance", "Max distance", 0, 0, 10000, 100, " m (0 = ∞)"));
        on(Events.Key.class, e -> {
            if (!e.isPress() || e.screenOpen()) return;
            if (addKey.triggeredBy(platform(), e.key())) addHere("Waypoint " + (waypoints.size() + 1));
            else if (manageKey.triggeredBy(platform(), e.key())) platform().openScreen(new WaypointScreen(core, this));
        });
        data.onChange(v -> { if (!saving && core != null && loaded) load(); });
    }


    @Override protected void onActivate() { load(); }

    // ---- Daten ----

    /** Eigene Datei, unabhängig von Profilen und vom "Zurücksetzen" im Menü. */
    static final String FILE = "waypoints.json";

    private boolean saving;

    private void load() {
        waypoints.clear();
        String json = core.config().readText(FILE);
        if (json != null) parseInto(json, waypoints);
        // Alte Versionen speicherten die Wegpunkte als Einstellung im Profil: einmalig übernehmen
        String legacy = data.get();
        if (legacy != null && !legacy.isBlank() && !legacy.trim().equals("[]")) {
            List<Waypoint> old = new ArrayList<>();
            parseInto(legacy, old);
            for (Waypoint w : old) if (!waypoints.contains(w)) waypoints.add(w);
            saving = true;
            try {
                data.set("[]");
            } finally {
                saving = false;
            }
            save();
        }
        loaded = true;
    }

    private static void parseInto(String json, List<Waypoint> out) {
        try {
            JsonElement el = JsonParser.parseString(json);
            if (!el.isJsonArray()) return;
            for (JsonElement e : el.getAsJsonArray()) {
                try {
                    JsonObject o = e.getAsJsonObject();
                    out.add(new Waypoint(o.get("name").getAsString(), o.get("x").getAsInt(), o.get("y").getAsInt(),
                            o.get("z").getAsInt(), o.get("world").getAsString(), o.get("dim").getAsString(),
                            o.get("color").getAsInt(), !o.has("visible") || o.get("visible").getAsBoolean(),
                            o.has("death") && o.get("death").getAsBoolean()));
                } catch (RuntimeException ignored) {
                    // einzelnen kaputten Eintrag überspringen, Rest behalten
                }
            }
        } catch (RuntimeException e) {
            Log.warn("Could not read waypoints: " + e.getMessage());
        }
    }

    private void save() {
        JsonArray arr = new JsonArray();
        for (Waypoint w : waypoints) {
            JsonObject o = new JsonObject();
            o.addProperty("name", w.name());
            o.addProperty("x", w.x());
            o.addProperty("y", w.y());
            o.addProperty("z", w.z());
            o.addProperty("world", w.world());
            o.addProperty("dim", w.dimension());
            o.addProperty("color", w.color());
            o.addProperty("visible", w.visible());
            o.addProperty("death", w.death());
            arr.add(o);
        }
        core.config().writeText(FILE, arr.toString());
    }

    public List<Waypoint> all() {
        if (!loaded) load();
        return List.copyOf(waypoints);
    }

    /** Wegpunkte der aktuellen Welt (alle Dimensionen). */
    public List<Waypoint> inThisWorld() {
        List<Waypoint> out = new ArrayList<>();
        String world = platform().worldKey();
        for (Waypoint w : all()) if (w.world().equals(world)) out.add(w);
        return out;
    }

    /** Sichtbare Wegpunkte in dieser Welt und Dimension. */
    public List<Waypoint> visibleHere() {
        List<Waypoint> out = new ArrayList<>();
        String world = platform().worldKey(), dim = platform().dimension();
        for (Waypoint w : all()) if (w.visible() && w.world().equals(world) && w.dimension().equals(dim)) out.add(w);
        return out;
    }

    public Waypoint addHere(String name) {
        Vec3 p = platform().playerPos();
        int color = PALETTE[waypoints.size() % PALETTE.length];
        Waypoint w = new Waypoint(name, (int) Math.floor(p.x()), (int) Math.floor(p.y()), (int) Math.floor(p.z()),
                platform().worldKey(), platform().dimension(), color, true, false);
        add(w);
        platform().notify("Waypoint \"" + name + "\" added");
        return w;
    }

    public void add(Waypoint w) {
        if (!loaded) load();
        waypoints.add(w);
        save();
    }

    public void replace(Waypoint old, Waypoint now) {
        int i = waypoints.indexOf(old);
        if (i >= 0) {
            waypoints.set(i, now);
            save();
        }
    }

    public void remove(Waypoint w) {
        if (waypoints.remove(w)) save();
    }

    // ---- Anzeige ----

    @Override public int width(Renderer r) { return 1; }

    @Override public int height(Renderer r) { return 1; }

    @Override protected boolean drawsDefaultBackground() { return false; }

    @Override public boolean hasContent() { return platform().inWorld() && !platform().isScreenOpen(); }

    /** Marker werden über den ganzen Bildschirm gezeichnet, nicht als verschiebbares Element. */
    @Override protected void renderContent(Renderer r, boolean preview) {}

    @Override public boolean editable() { return false; }

    /** Vom HUD-Manager jeden Frame aufgerufen. */
    @Override public void renderOverlay(Renderer r, float partialTick) {
        if (!isActive() || !hasContent()) return;
        drawMarkers(r, partialTick);
    }

    /**
     * Wegpunkte, die gerade angezeigt werden: alle sichtbaren, wenn das Modul an ist; sonst nur der
     * Todespunkt, falls "Death Waypoint" an ist (der funktioniert auch ohne dieses Modul).
     */
    public List<Waypoint> shownHere() {
        if (core == null) return List.of();
        if (isActive()) return visibleHere();
        DeathPointModule death = core.module(DeathPointModule.class);
        if (death == null || !death.isActive()) return List.of();
        List<Waypoint> out = new ArrayList<>();
        for (Waypoint w : visibleHere()) if (w.death()) out.add(w);
        return out;
    }

    /** Radius (px) um das Fadenkreuz, in dem ein Punkt als "angezielt" gilt. */
    static final float HOVER_RADIUS = 9;

    /**
     * Marker wie bei Xaero: im Bild nur ein kleiner Punkt; zielt man mit dem Fadenkreuz darauf,
     * erscheinen Name und Entfernung (nur beim nächstgelegenen angezielten Punkt).
     */
    public void drawMarkers(Renderer r, float partialTick) {
        if (!platform().inWorld() || platform().isScreenOpen()) return;
        List<Waypoint> list = shownHere();
        if (list.isEmpty()) return;
        // exakt die Kamera, mit der die Welt gezeichnet wurde (inkl. Laufwackeln) → kein Zittern
        CameraView cam = platform().camera(partialTick);
        Vec3 pos = platform().playerPos();
        float w = r.screenWidth(), h = r.screenHeight(), cx = w / 2f, cy = h / 2f;
        double max = maxDistance.get();

        Waypoint hovered = null;
        float hx = 0, hy = 0, best = Float.MAX_VALUE;
        double hoveredDist = 0;
        for (Waypoint wp : list) {
            double dist = wp.distanceTo(pos.x(), pos.y(), pos.z());
            if (max > 0 && dist > max && !wp.death()) continue;
            Projection.Point pt = Projection.project(wp.x() + 0.5, wp.y() + 1.0, wp.z() + 0.5, cam, w, h);
            float x = pt.x(), y = pt.y();
            boolean edge = !pt.onScreen();
            if (edge) {
                if (!edgeMarkers.on()) continue;
                x = Math.max(6, Math.min(w - 6, x));
                y = Math.max(6, Math.min(h - 6, y));
            }
            drawDot(r, wp.color() | 0xFF000000, x, y, edge);
            if (!edge) {
                float d = (x - cx) * (x - cx) + (y - cy) * (y - cy);
                if (d <= HOVER_RADIUS * HOVER_RADIUS && d < best) {
                    best = d;
                    hovered = wp;
                    hx = x;
                    hy = y;
                    hoveredDist = dist;
                }
            }
        }
        if (hovered != null) drawLabel(r, hovered, hoveredDist, hx, hy);
    }

    private static void drawDot(Renderer r, int color, float x, float y, boolean edge) {
        int s = edge ? 2 : 3;
        diamond(r, x, y, s + 1, 0xB0000000); // dunkler Rand, gut sichtbar auf hellem und dunklem Grund
        diamond(r, x, y, s, color);
    }

    /** Raute aus Pixelzeilen, Mittelpunkt (x, y), Radius s. */
    private static void diamond(Renderer r, float x, float y, int s, int color) {
        x = Math.round(x);
        y = Math.round(y);
        for (int dy = -s; dy < s; dy++) {
            int half = s - (dy < 0 ? -dy - 1 : dy);
            r.fill(x - half, y + dy, x + half, y + dy + 1, color);
        }
    }

    private void drawLabel(Renderer r, Waypoint wp, double dist, float x, float y) {
        String label = wp.name() + (showDistance.on() ? "  " + formatDistance(dist) : "");
        float tw = r.textWidth(label);
        float top = y - 7 - r.fontHeight() - 3;
        r.fill(x - tw / 2 - 3, top, x + tw / 2 + 3, top + r.fontHeight() + 3, 0xA0000000);
        r.text(label, x - tw / 2, top + 2, wp.color() | 0xFF000000, shadow());
    }

    public static String formatDistance(double d) {
        return d >= 1000 ? String.format(Locale.ROOT, "%.1f km", d / 1000) : Math.round(d) + " m";
    }
}
