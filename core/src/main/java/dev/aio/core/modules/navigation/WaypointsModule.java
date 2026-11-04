package dev.aio.core.modules.navigation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aio.core.event.Events;
import dev.aio.core.hud.HudElement;
import dev.aio.core.module.Category;
import dev.aio.core.modules.render.FreelookModule;
import dev.aio.core.modules.render.ZoomModule;
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
        super("waypoints", "Wegpunkte", "Wegpunkte mit Entfernung im Bild und auf dem Kompass", Category.NAVIGATION, false, 0, 0);
        data = add(new StringSetting("data", "Daten", "[]", 1_000_000));
        data.hidden();
        posX.hidden();
        posY.hidden();
        addKey = add(new KeybindSetting("addKey", "Wegpunkt setzen", "key.keyboard.b"));
        manageKey = add(new KeybindSetting("manageKey", "Wegpunkte verwalten", "key.keyboard.n"));
        add(new ActionSetting("manage", "Wegpunkte verwalten", () -> core.platform().openScreen(new WaypointScreen(core, this))));
        showDistance = add(new BooleanSetting("distance", "Entfernung zeigen", true));
        edgeMarkers = add(new BooleanSetting("edge", "Am Bildrand zeigen", true));
        maxDistance = add(new NumberSetting("maxDistance", "Max. Entfernung", 0, 0, 10000, 100, " m (0 = ∞)"));
        on(Events.Key.class, e -> {
            if (!e.isPress() || e.screenOpen()) return;
            if (addKey.matches(e.key())) addHere("Wegpunkt " + (waypoints.size() + 1));
            else if (manageKey.matches(e.key())) platform().openScreen(new WaypointScreen(core, this));
        });
        data.onChange(v -> { if (!saving) load(); });
    }

    @Override public boolean enabledByDefault() { return true; }

    @Override protected void onActivate() { load(); }

    // ---- Daten ----

    private boolean saving;

    private void load() {
        waypoints.clear();
        try {
            JsonElement el = JsonParser.parseString(data.get());
            if (el.isJsonArray()) {
                for (JsonElement e : el.getAsJsonArray()) {
                    JsonObject o = e.getAsJsonObject();
                    waypoints.add(new Waypoint(o.get("name").getAsString(), o.get("x").getAsInt(), o.get("y").getAsInt(),
                            o.get("z").getAsInt(), o.get("world").getAsString(), o.get("dim").getAsString(),
                            o.get("color").getAsInt(), !o.has("visible") || o.get("visible").getAsBoolean(),
                            o.has("death") && o.get("death").getAsBoolean()));
                }
            }
        } catch (RuntimeException e) {
            Log.warn("Wegpunkte konnten nicht gelesen werden: " + e.getMessage());
        }
        loaded = true;
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
        saving = true;
        try {
            data.set(arr.toString());
        } finally {
            saving = false;
        }
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
        platform().notify("Wegpunkt \"" + name + "\" gesetzt");
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
    @Override public void renderOverlay(Renderer r) {
        if (!isActive() || !hasContent()) return;
        Vec3 eye = platform().eyePos();
        Vec3 pos = platform().playerPos();
        FreelookModule fl = core.module(FreelookModule.class);
        boolean free = fl != null && fl.isEngaged();
        float yaw = free ? fl.cameraYaw() : platform().playerYaw();
        float pitch = free ? fl.cameraPitch() : platform().playerPitch();
        ZoomModule zoom = core.module(ZoomModule.class);
        double fov = platform().fov() * (zoom != null ? zoom.currentMultiplier() : 1);
        float w = r.screenWidth(), h = r.screenHeight();
        double max = maxDistance.get();

        for (Waypoint wp : visibleHere()) {
            double dist = wp.distanceTo(pos.x(), pos.y(), pos.z());
            if (max > 0 && dist > max) continue;
            Projection.Point pt = Projection.project(wp.x() + 0.5, wp.y() + 1.0, wp.z() + 0.5, eye.x(), eye.y(), eye.z(),
                    yaw, pitch, fov, w, h);
            float x = pt.x(), y = pt.y();
            boolean edge = !pt.onScreen();
            if (edge) {
                if (!edgeMarkers.on()) continue;
                x = Math.max(8, Math.min(w - 8, x));
                y = Math.max(8, Math.min(h - 20, y));
            }
            drawMarker(r, wp, dist, x, y, edge);
        }
    }

    private void drawMarker(Renderer r, Waypoint wp, double dist, float x, float y, boolean edge) {
        int color = wp.color() | 0xFF000000;
        // Raute
        r.fill(x - 1, y - 4, x + 1, y + 4, color);
        r.fill(x - 2, y - 3, x + 2, y + 3, color);
        r.fill(x - 3, y - 2, x + 3, y + 2, color);
        r.fill(x - 4, y - 1, x + 4, y + 1, color);
        if (edge) return;
        String label = wp.name() + (showDistance.on() ? " (" + formatDistance(dist) + ")" : "");
        float tw = r.textWidth(label);
        r.fill(x - tw / 2 - 2, y - 15, x + tw / 2 + 2, y - 5, 0x90000000);
        r.text(label, x - tw / 2, y - 14, color, shadow());
    }

    public static String formatDistance(double d) {
        return d >= 1000 ? String.format(Locale.ROOT, "%.1f km", d / 1000) : Math.round(d) + " m";
    }
}
