package dev.aio.core.modules.navigation;

import dev.aio.core.event.Events;
import dev.aio.core.hud.HudElement;
import dev.aio.core.i18n.Lang;
import dev.aio.core.module.Category;
import dev.aio.core.platform.CameraView;
import dev.aio.core.platform.Renderer;
import dev.aio.core.platform.Vec3;
import dev.aio.core.setting.ActionSetting;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.EnumSetting;
import dev.aio.core.setting.KeybindSetting;
import dev.aio.core.setting.NumberSetting;
import dev.aio.core.setting.StringSetting;
import dev.aio.core.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
    private final BooleanSetting alwaysNames;
    private final EnumSetting<MarkerStyle> markerStyle;

    public enum MarkerStyle implements EnumSetting.Named {
        DOT("Dot"), ICON("Icon with letter");

        private final String displayName;

        MarkerStyle(String displayName) { this.displayName = displayName; }

        @Override public String displayName() { return displayName; }
    }
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
        manageKey = add(new KeybindSetting("manageKey", "Open waypoint list", "key.keyboard.n"));
        add(new ActionSetting("manage", "Waypoint list", () -> core.platform().openScreen(new WaypointScreen(core, this))));
        showDistance = add(new BooleanSetting("distance", "Show distance", true));
        edgeMarkers = add(new BooleanSetting("edge", "Show at screen edge", true));
        maxDistance = add(new NumberSetting("maxDistance", "Max distance", 0, 0, 10000, 100, " m (0 = ∞)"));
        markerStyle = add(new EnumSetting<>("markerStyle", "Marker", MarkerStyle.DOT));
        alwaysNames = add(new BooleanSetting("alwaysNames", "Always show names", false));
        importShared = add(new BooleanSetting("importShared", "Add shared waypoints automatically", false));
        on(Events.ChatReceived.class, e -> { if (importShared.on() && !e.fromSelf()) importShared(e.text()); });
        on(Events.Key.class, e -> {
            if (!e.isPress() || e.screenOpen()) return;
            if (addKey.triggeredBy(platform(), e.key())) platform().openScreen(new WaypointEditScreen(core, this, null));
            else if (manageKey.triggeredBy(platform(), e.key())) platform().openScreen(new WaypointScreen(core, this));
        });
        data.onChange(v -> { if (!saving && core != null && loaded) load(); });
    }


    private final BooleanSetting importShared;

    /** Im Chat geteilte Wegpunkte automatisch übernehmen (nur wenn eingeschaltet). */
    private void importShared(String text) {
        for (SharedWaypoint.Found f : SharedWaypoint.parse(text)) addShared(f);
    }

    /** Fügt einen geteilten Wegpunkt hinzu (Klick im Chat oder automatisch); doppelte werden übersprungen. */
    public boolean addShared(SharedWaypoint.Found f) {
        String dim = f.dimension() != null ? f.dimension() : platform().dimension();
        int y = f.y() != null ? f.y() : (int) Math.floor(platform().playerPos().y());
        for (Waypoint w : inThisWorld()) {
            if (w.x() == f.x() && w.z() == f.z() && w.dimension().equals(dim)) {
                platform().notify(dev.aio.core.i18n.Lang.tr("aio.ui.waypoints.shared_known", "Waypoint \"%s\" already exists", w.name()));
                return false;
            }
        }
        add(new Waypoint(f.name(), f.x(), y, f.z(), platform().worldKey(), dim, f.color(), true, false));
        platform().notify(dev.aio.core.i18n.Lang.tr("aio.ui.waypoints.shared_added", "Waypoint \"%s\" added (%s %s %s)", f.name(), f.x(), y, f.z()));
        return true;
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
        platform().notify(Lang.tr("aio.ui.waypoints.waypoint_added", "Waypoint \"%s\" added", name));
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
        DeathPointModule death = core.module(DeathPointModule.class);
        boolean showDeath = death != null && death.isActive();
        if (!isActive() && !showDeath) return List.of();
        // Todespunkte werden immer gespeichert, aber nur gezeigt, wenn "Death Waypoint" an ist
        List<Waypoint> out = new ArrayList<>();
        for (Waypoint w : visibleHere()) if (w.death() ? showDeath : isActive()) out.add(w);
        return out;
    }

    /** Radius (px) um das Fadenkreuz, in dem ein Punkt als "angezielt" gilt. */
    static final float HOVER_RADIUS = 10;

    /** Einblend-Zustand der Beschriftung je Wegpunkt (0..1), für weiches Ein-/Ausblenden. */
    private final Map<String, Float> labelFade = new HashMap<>();
    private long lastFrame;

    private static String key(Waypoint w) { return w.name() + '|' + w.x() + '|' + w.y() + '|' + w.z() + '|' + w.dimension(); }

    /**
     * Marker im Stil der großen Wegpunkt-Mods, nur aufgeräumter:
     * <ul>
     *   <li>Im Bild ein kleiner Punkt (oder auf Wunsch ein Symbol mit Anfangsbuchstaben wie bei Xaero).</li>
     *   <li>Zielt man darauf (oder "Always show names"), blendet die Beschriftung weich ein: Name in
     *       der Wegpunktfarbe, Entfernung dahinter in Grau (wie JourneyMap).</li>
     *   <li>Der angezielte Marker wird etwas größer, nahe Marker (< 4 Blöcke) blenden aus.</li>
     *   <li>Außerhalb des Bildes ein kleiner Pfeil am Rand, der in die Richtung zeigt.</li>
     * </ul>
     */
    public void drawMarkers(Renderer r, float partialTick) {
        if (!platform().inWorld() || platform().isScreenOpen()) return;
        List<Waypoint> list = shownHere();
        long now = System.currentTimeMillis();
        float dt = lastFrame == 0 ? 0 : Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        if (list.isEmpty()) return;
        // exakt die Kamera, mit der die Welt gezeichnet wurde (inkl. Laufwackeln) → kein Zittern
        CameraView cam = platform().camera(partialTick);
        Vec3 pos = platform().playerPos();
        float w = r.screenWidth(), h = r.screenHeight(), cx = w / 2f, cy = h / 2f;
        double max = maxDistance.get();

        // 1. Projektion und angezielten Marker bestimmen
        record M(Waypoint wp, float x, float y, boolean edge, double dist) {}
        List<M> ms = new ArrayList<>();
        M hovered = null;
        float best = Float.MAX_VALUE;
        for (Waypoint wp : list) {
            double dist = wp.distanceTo(pos.x(), pos.y(), pos.z());
            if (max > 0 && dist > max && !wp.death()) continue;
            Projection.Point pt = Projection.project(wp.x() + 0.5, wp.y() + 1.0, wp.z() + 0.5, cam, w, h);
            boolean edge = !pt.onScreen();
            float x = pt.x(), y = pt.y();
            if (edge) {
                if (!edgeMarkers.on()) continue;
                // vom Bildschirmmittelpunkt Richtung Ziel bis an den Rand
                float dx = x - cx, dy = y - cy;
                float m = 10, k = Math.min(Math.abs(dx) > 0.001f ? (cx - m) / Math.abs(dx) : Float.MAX_VALUE,
                        Math.abs(dy) > 0.001f ? (cy - m) / Math.abs(dy) : Float.MAX_VALUE);
                x = cx + dx * k;
                y = cy + dy * k;
            }
            M mk = new M(wp, x, y, edge, dist);
            ms.add(mk);
            if (!edge) {
                float d = (x - cx) * (x - cx) + (y - cy) * (y - cy);
                if (d <= HOVER_RADIUS * HOVER_RADIUS && d < best) {
                    best = d;
                    hovered = mk;
                }
            }
        }

        // 2. Weiter entfernte zuerst zeichnen, damit nahe darüber liegen
        ms.sort((a, b2) -> Double.compare(b2.dist, a.dist));
        Set<String> alive = new HashSet<>();
        for (M m : ms) {
            String k = key(m.wp);
            alive.add(k);
            boolean target = m == hovered || (alwaysNames.on() && !m.edge);
            float f = labelFade.getOrDefault(k, 0f);
            f = Math.max(0, Math.min(1, f + (target ? dt * 8 : -dt * 6)));
            labelFade.put(k, f);
            // nahe Wegpunkte blenden aus (man steht ja schon dort)
            float near = (float) Math.max(0, Math.min(1, (m.dist - 2.5) / 3));
            int color = m.wp.color() | 0xFF000000;
            if (m.edge) drawArrow(r, color, m.x, m.y, m.x - cx, m.y - cy);
            else drawMarker(r, m.wp, color, m.x, m.y, near, m == hovered);
            if (!m.edge && f > 0.02f && near > 0.05f) drawLabel(r, m.wp, m.dist, m.x, m.y, f * near);
        }
        labelFade.keySet().retainAll(alive);
    }

    private static int alpha(int argb, float a) {
        int base = (argb >>> 24) & 0xFF;
        return (Math.round(base * Math.max(0, Math.min(1, a))) << 24) | (argb & 0xFFFFFF);
    }

    private void drawMarker(Renderer r, Waypoint wp, int color, float x, float y, float a, boolean hovered) {
        if (a <= 0.02f) return;
        r.push();
        r.translate(x, y);
        if (hovered) r.scale(1.3f);
        if (markerStyle.get() == MarkerStyle.ICON) {
            // Quadrat mit Anfangsbuchstaben, wie bei Xaero
            String letter = wp.death() ? "✖" : wp.name().isEmpty() ? "?" : wp.name().substring(0, 1).toUpperCase(Locale.ROOT);
            int s = 5;
            r.fill(-s - 1, -s - 1, s + 1, s + 1, alpha(0xC0000000, a));
            r.fill(-s, -s, s, s, alpha(color, a));
            int lum = ((color >> 16 & 0xFF) * 3 + (color >> 8 & 0xFF) * 6 + (color & 0xFF)) / 10;
            r.text(letter, -r.textWidth(letter) / 2f + 0.5f, -3.5f, alpha(lum > 140 ? 0xFF000000 : 0xFFFFFFFF, a), false);
        } else {
            int s = wp.death() ? 4 : 3;
            diamond(r, s + 1, alpha(0xB0000000, a)); // dunkler Rand: sichtbar auf hellem und dunklem Grund
            diamond(r, s, alpha(color, a));
            r.fill(-1, -1, 0, 0, alpha(0x60FFFFFF, a)); // kleiner Glanzpunkt
        }
        r.pop();
    }

    /** Kleiner Pfeil am Bildrand, zeigt grob in die Richtung (vier Grundrichtungen). */
    private static void drawArrow(Renderer r, int color, float x, float y, float dx, float dy) {
        r.push();
        r.translate(x, y);
        int dir = Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? 0 : 2) : (dy > 0 ? 1 : 3); // 0 rechts, 1 unten, 2 links, 3 oben
        for (int pass = 0; pass < 2; pass++) {
            int c = pass == 0 ? 0xB0000000 : color;
            int s = pass == 0 ? 5 : 4;
            for (int i = 0; i < s; i++) {
                int half = s - 1 - i;
                switch (dir) {
                    case 0 -> r.fill(i - 2, -half - 1, i - 1, half + 1, c);
                    case 2 -> r.fill(1 - i, -half - 1, 2 - i, half + 1, c);
                    case 1 -> r.fill(-half - 1, i - 2, half + 1, i - 1, c);
                    default -> r.fill(-half - 1, 1 - i, half + 1, 2 - i, c);
                }
            }
        }
        r.pop();
    }

    /** Raute aus Pixelzeilen um (0, 0), Radius s. */
    private static void diamond(Renderer r, int s, int color) {
        for (int dy = -s; dy < s; dy++) {
            int half = s - (dy < 0 ? -dy - 1 : dy);
            r.fill(-half, dy, half, dy + 1, color);
        }
    }

    private void drawLabel(Renderer r, Waypoint wp, double dist, float x, float y, float a) {
        String name = wp.name();
        String d = showDistance.on() ? formatDistance(dist) : "";
        int nw = r.textWidth(name), dw = d.isEmpty() ? 0 : r.textWidth(d) + 5;
        int tw = nw + dw;
        int fh = r.fontHeight();
        float lift = (1 - a) * 3; // gleitet beim Einblenden leicht nach oben
        r.push();
        r.translate(x, y - 9 - fh + lift);
        int x1 = -tw / 2 - 4, x2 = tw - tw / 2 + 4, y1 = -3, y2 = fh + 1;
        int bg = alpha(0xB0101014, a);
        r.fill(x1 + 1, y1, x2 - 1, y2, bg); // leicht abgerundete Ecken
        r.fill(x1, y1 + 1, x1 + 1, y2 - 1, bg);
        r.fill(x2 - 1, y1 + 1, x2, y2 - 1, bg);
        r.fill(x1 + 1, y2 - 1, x2 - 1, y2, alpha(wp.color() | 0xFF000000, a * 0.9f)); // Farbstreifen unten
        r.text(name, -tw / 2f, 0, alpha(0xFFFFFFFF, a), shadow());
        if (!d.isEmpty()) r.text(d, -tw / 2f + nw + 5, 0, alpha(0xFFAAAAAA, a), shadow());
        r.pop();
    }

    public static String formatDistance(double d) {
        return d >= 1000 ? String.format(Locale.ROOT, "%.1f km", d / 1000) : Math.round(d) + " m";
    }
}
