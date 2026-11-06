package dev.aio.core.modules.map;

import dev.aio.core.event.Events;
import dev.aio.core.hud.HudElement;
import dev.aio.core.module.Category;
import dev.aio.core.modules.navigation.Waypoint;
import dev.aio.core.modules.navigation.WaypointsModule;
import dev.aio.core.platform.NametagTarget;
import dev.aio.core.platform.Renderer;
import dev.aio.core.platform.Vec3;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.EnumSetting;
import dev.aio.core.setting.NumberSetting;
import dev.aio.core.ui.Draw;
import dev.aio.core.ui.Theme;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Minimap im HUD (eckig oder rund, flach oder als schräges "Diorama"). Die Welt wird beim Spielen
 * langsam mitgelesen und pro Welt und Dimension gespeichert. Das Bild wird nur bei Änderung neu
 * gebacken und pro Frame als eine Textur gezeichnet.
 */
public final class MapModule extends HudElement {
    public enum Shape implements EnumSetting.Named {
        SQUARE("Square"), ROUND("Round");

        private final String displayName;

        Shape(String displayName) { this.displayName = displayName; }

        @Override public String displayName() { return displayName; }
    }

    private static final int RADIUS = 6; // Chunks um den Spieler, die gelesen werden

    private final EnumSetting<Shape> shape;
    private final NumberSetting size, blocks;
    private final BooleanSetting diorama, radar, waypoints;
    private MapStore store;
    private final List<int[]> offsets = new ArrayList<>();
    private int scanIndex, nearIndex, ticks;

    public MapModule() {
        super("map", "Map", "Minimap with a slanted diorama look", Category.QOL, false, 1.0, 0.0);
        shape = add(new EnumSetting<>("shape", "Shape", Shape.SQUARE));
        size = add(new NumberSetting("size", "Size", 110, 64, 180, 2, " px"));
        blocks = add(new NumberSetting("blocks", "View width", 64, 24, 192, 4, " blocks"));
        diorama = add(new BooleanSetting("diorama", "Diorama look (slanted)", true));
        radar = add(new BooleanSetting("radar", "Players and mobs", true));
        waypoints = add(new BooleanSetting("waypoints", "Waypoints", true));
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) offsets.add(new int[]{x, z});
        }
        offsets.sort(Comparator.comparingInt(o -> o[0] * o[0] + o[1] * o[1]));
        on(Events.Tick.class, e -> tick());
        on(Events.ServerLeave.class, e -> { if (store != null) store.save(); });
    }

    @Override protected boolean drawsDefaultBackground() { return false; }

    @Override protected boolean defaultBackground() { return false; }

    @Override public int width(Renderer r) { return size.intValue(); }

    @Override public int height(Renderer r) { return size.intValue() + 11; }

    @Override public boolean hasContent() { return platform().inWorld(); }

    @Override protected void onDeactivate() {
        if (store != null) store.save();
    }

    public MapStore store() {
        if (store == null) store = new MapStore(core.config().root().resolve("map"));
        return store;
    }


    // ---- Welt lesen ----

    private int[] buf = new int[0];
    private int bakedVersion = -1, bakedKey, imageVersion;
    private long lastBake;
    private List<NametagTarget> radarCache = List.of();

    private void tick() {
        if (!platform().inWorld()) return;
        MapStore s = store();
        s.switchTo(platform().worldKey(), platform().dimension());
        Vec3 p = platform().playerPos();
        int pcx = (int) Math.floor(p.x()) >> 4, pcz = (int) Math.floor(p.z()) >> 4, py = (int) Math.floor(p.y());
        // ein Chunk alle 2 Ticks (16 × 16 Säulen), die Chunks direkt um den Spieler öfter
        if (ticks % 2 == 0) {
            int[] o = offsets.get(scanIndex++ % offsets.size());
            scanChunk(s, pcx + o[0], pcz + o[1], py);
        }
        if (ticks % 10 == 0) {
            int[] o = offsets.get(nearIndex++ % 9);
            scanChunk(s, pcx + o[0], pcz + o[1], py);
        }
        if (ticks % 6 == 0 && radar.on()) {
            radarCache = platform().nametagTargets(blocks.intValue() * 0.75, 0f, true, true, false, false, true);
        }
        if (++ticks % 1200 == 0) s.save();
    }

    private void scanChunk(MapStore s, int cx, int cz, int refY) {
        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                int x = cx * 16 + lx, z = cz * 16 + lz;
                long c = platform().mapColumn(x, z, refY);
                if (c != 0) s.put(x, z, (int) c, (int) (c >>> 32) - 1024);
            }
        }
    }

    // ---- Minimap ----

    @Override protected void renderContent(Renderer r, boolean preview) {
        int sz = size.intValue();
        MapStore s = store();
        Vec3 p = platform().playerPos();
        int bx = (int) Math.floor(p.x()), bz = (int) Math.floor(p.z());
        int refH = s.rgb(bx, bz) == 0 ? (int) p.y() : s.height(bx, bz);
        boolean round = shape.get() == Shape.ROUND;
        // Mitte immer auf dem Block des Spielers: das Bild muss nur beim Blockwechsel neu gebacken werden
        MapView v = new MapView(0, 0, sz, sz, bx + 0.5, bz + 0.5, sz / (float) blocks.intValue(), refH, diorama.on());
        int key = Objects.hash(bx, bz, sz, blocks.intValue(), diorama.on(), round, refH / 2);
        long now = System.currentTimeMillis();
        if ((key != bakedKey || s.version != bakedVersion) && now - lastBake >= 120) {
            if (buf.length != sz * sz) buf = new int[sz * sz];
            MapRenderer.bake(buf, sz, s, v, round);
            bakedKey = key;
            bakedVersion = s.version;
            lastBake = now;
            imageVersion++;
        }
        int accent = Theme.accent(core);
        if (round) {
            r.fill(sz * 0.14f, 0, sz * 0.86f, sz, 0xC0080A0E);
            r.fill(0, sz * 0.14f, sz, sz * 0.86f, 0xC0080A0E);
            r.fill(sz * 0.04f, sz * 0.04f, sz * 0.96f, sz * 0.96f, 0xC0080A0E);
        } else {
            Draw.round(r, -1, -1, sz + 1, sz + 1, 2, 0xC0080A0E);
        }
        if (buf.length == sz * sz) r.image("minimap", buf, sz, sz, imageVersion, 0, 0, sz, sz);
        markers(r, v, p, round);
        MapRenderer.player(r, v.sx(p.x()), v.sy(p.z(), refH), platform().playerYaw());
        if (round) {
            ring(r, sz, accent);
        } else {
            Draw.roundOutline(r, -1, -1, sz + 1, sz + 1, accent & 0x00FFFFFF | 0x90000000);
        }
        r.centeredText("N", sz / 2f, 2, 0xFFFFFFFF, true);
        String pos = bx + ", " + (int) Math.floor(p.y()) + ", " + bz;
        r.centeredText(pos, sz / 2f, sz + 2, Theme.TEXT, true);
    }

    /** Dünner Ring um die runde Karte aus kurzen Stücken. */
    private static void ring(Renderer r, int sz, int accent) {
        float c = sz / 2f, rad = sz / 2f;
        int col = accent & 0x00FFFFFF | 0x90000000;
        for (int i = 0; i < 120; i++) {
            double a = i / 120.0 * Math.PI * 2;
            float x = c + (float) Math.cos(a) * rad, y = c + (float) Math.sin(a) * rad;
            r.fill(x - 1, y - 1, x + 1, y + 1, col);
        }
    }

    private void markers(Renderer r, MapView v, Vec3 p, boolean round) {
        MapStore s = store();
        if (waypoints.on()) drawWaypoints(r, v);
        if (radar.on()) {
            for (NametagTarget t : radarCache) {
                int c = t.kind() == NametagTarget.Kind.PLAYER ? 0xFF55AAFF : 0xFFFF5555;
                int tx = (int) Math.floor(t.x()), tz = (int) Math.floor(t.z());
                int h = s.rgb(tx, tz) == 0 ? v.refH() : s.height(tx, tz);
                float x = v.sx(t.x()), y = v.sy(t.z(), Math.max(h, (int) t.y()));
                if (!round || inside(v, x, y)) MapRenderer.dot(r, x, y, 1.2f, c);
            }
        }
    }

    private static boolean inside(MapView v, float x, float y) {
        float rad = Math.min(v.w(), v.h()) / 2f, dx = x - (v.x0() + v.w() / 2f), dy = y - (v.y0() + v.h() / 2f);
        return dx * dx + dy * dy <= rad * rad;
    }

    /** Wegpunkte (auch Todespunkte) als Punkte; was außerhalb liegt, sitzt am Rand. */
    private void drawWaypoints(Renderer r, MapView v) {
        WaypointsModule wp = core.module(WaypointsModule.class);
        if (wp == null) return;
        for (Waypoint w : wp.shownHere()) {
            float x = v.sx(w.x() + 0.5), y = v.sy(w.z() + 0.5, w.y());
            x = Math.max(3, Math.min(v.w() - 3, x));
            y = Math.max(3, Math.min(v.h() - 3, y));
            MapRenderer.dot(r, x, y, w.death() ? 2.4f : 2f, 0xFF000000 | w.color());
        }
    }
}
