package dev.aio.core.modules.hud;

import dev.aio.core.event.Events;
import dev.aio.core.hud.HudElement;
import dev.aio.core.i18n.Lang;
import dev.aio.core.module.Category;
import dev.aio.core.platform.Renderer;
import dev.aio.core.platform.Vec3;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.util.ServerTps;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Info Lines (like MiniHUD): a list of extra information, every line can be switched on or off.
 * Updated once per tick.
 */
public final class InfoLinesModule extends HudElement {
    private static final int LINE = 10;
    private static final int SPEED_TICKS = 10;

    private final BooleanSetting tps, speed, chunk, region, facing, light, moon, entities;
    private final Vec3[] positions = new Vec3[SPEED_TICKS + 1];
    private int posCount, posNext;
    private List<String> lines = List.of();
    private int cachedW = -1;

    public InfoLinesModule() {
        super("infolines", "Info Lines", "Extra info like MiniHUD: TPS, speed, chunk, facing, light, moon, entities", Category.HUD, false, 0.0, 0.22);
        tps = add(new BooleanSetting("tps", "TPS / server lag", true));
        speed = add(new BooleanSetting("speed", "Speed (m/s)", true));
        chunk = add(new BooleanSetting("chunk", "Chunk", true));
        region = add(new BooleanSetting("region", "Region file", false));
        facing = add(new BooleanSetting("facing", "Facing in degrees", true));
        light = add(new BooleanSetting("light", "Light level", true));
        moon = add(new BooleanSetting("moon", "Moon phase", false));
        entities = add(new BooleanSetting("entities", "Entity count", false));
        on(Events.ServerJoin.class, e -> {
            ServerTps.reset();
            posCount = 0;
        });
    }

    @Override protected void onActivate() { update(); }

    @Override public void update() {
        List<String> next = new ArrayList<>();
        if (platform().inWorld()) {
            Vec3 p = platform().playerPos();
            positions[posNext] = p;
            posNext = (posNext + 1) % positions.length;
            posCount = Math.min(positions.length, posCount + 1);

            if (tps.on()) next.add(tpsLine());
            if (speed.on()) next.add(Lang.tr("aio.ui.infolines.speed", "Speed: %s m/s", fmt1(speed())));
            int bx = (int) Math.floor(p.x()), bz = (int) Math.floor(p.z());
            int cx = bx >> 4, cz = bz >> 4;
            if (chunk.on()) next.add(Lang.tr("aio.ui.infolines.chunk", "Chunk: %s %s (in chunk: %s %s)", cx, cz, bx & 15, bz & 15));
            if (region.on()) next.add(Lang.tr("aio.ui.infolines.region", "Region: r.%s.%s.mca", cx >> 5, cz >> 5));
            if (facing.on()) {
                float yaw = wrap(platform().playerYaw());
                next.add(Lang.tr("aio.ui.infolines.facing", "Facing: %s (%s° / %s°)",
                        CoordinatesModule.direction(yaw), fmt1(yaw), fmt1(platform().playerPitch())));
            }
            if (light.on()) {
                int[] l = platform().lightLevels();
                if (l != null) next.add(Lang.tr("aio.ui.infolines.light", "Light: %s (block %s, sky %s)", Math.max(l[0], l[1]), l[0], l[1]));
            }
            if (moon.on()) next.add(Lang.tr("aio.ui.infolines.moon", "Moon: %s", moonName(Math.floorMod(platform().dayTime() / 24000L, 8))));
            if (entities.on()) {
                int n = platform().entityCount();
                if (n >= 0) next.add(Lang.tr("aio.ui.infolines.entities", "Entities: %s", n));
            }
        } else {
            posCount = 0;
        }
        if (!next.equals(lines)) {
            if (next.size() != lines.size()) cachedW = -1;
            else for (int i = 0; i < next.size(); i++) if (next.get(i).length() > lines.get(i).length()) cachedW = -1;
            lines = next;
        }
    }

    private String tpsLine() {
        if (platform().isSingleplayer()) return Lang.tr("aio.ui.infolines.tps", "TPS: %s", "20.0");
        double t = ServerTps.tps();
        String s = Lang.tr("aio.ui.infolines.tps", "TPS: %s", Double.isNaN(t) ? "…" : fmt1(t));
        double lag = ServerTps.secondsSinceUpdate();
        if (!Double.isNaN(lag) && lag > 2.5) s += " " + Lang.tr("aio.ui.infolines.lag", "(lag %ss)", fmt1(lag));
        return s;
    }

    /** Average speed over the last half second in blocks (= metres) per second. */
    private double speed() {
        if (posCount < 2) return 0;
        Vec3 now = positions[Math.floorMod(posNext - 1, positions.length)];
        Vec3 old = positions[Math.floorMod(posNext - posCount, positions.length)];
        double dx = now.x() - old.x(), dy = now.y() - old.y(), dz = now.z() - old.z();
        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (d > 64) return 0; // teleported
        return d * 20.0 / (posCount - 1);
    }

    static float wrap(float yaw) {
        float y = yaw % 360f;
        if (y >= 180f) y -= 360f;
        if (y < -180f) y += 360f;
        return y;
    }

    private static String fmt1(double v) { return String.format(Locale.ROOT, "%.1f", v); }

    static String moonName(long phase) {
        return switch ((int) phase) {
            case 0 -> Lang.tr("aio.ui.infolines.moon.full", "Full moon");
            case 1 -> Lang.tr("aio.ui.infolines.moon.waning_gibbous", "Waning gibbous");
            case 2 -> Lang.tr("aio.ui.infolines.moon.last_quarter", "Last quarter");
            case 3 -> Lang.tr("aio.ui.infolines.moon.waning_crescent", "Waning crescent");
            case 4 -> Lang.tr("aio.ui.infolines.moon.new", "New moon");
            case 5 -> Lang.tr("aio.ui.infolines.moon.waxing_crescent", "Waxing crescent");
            case 6 -> Lang.tr("aio.ui.infolines.moon.first_quarter", "First quarter");
            default -> Lang.tr("aio.ui.infolines.moon.waxing_gibbous", "Waxing gibbous");
        };
    }

    @Override public boolean hasContent() { return !lines.isEmpty(); }

    private List<String> shown(boolean preview) {
        return lines.isEmpty() && preview
                ? List.of("TPS: 19.9", "Speed: 5.6 m/s", "Chunk: 8 -16 (in chunk: 3 11)", "Facing: N (180.0° / 12.5°)", "Light: 15 (block 0, sky 15)")
                : lines;
    }

    @Override public int width(Renderer r) {
        if (cachedW < 0 || lines.isEmpty()) {
            int w = 0;
            for (String l : shown(true)) w = Math.max(w, r.textWidth(l));
            cachedW = w + PADDING * 2;
        }
        return cachedW;
    }

    @Override public int height(Renderer r) { return PADDING * 2 + shown(true).size() * LINE - 2; }

    @Override protected void renderContent(Renderer r, boolean preview) {
        List<String> s = shown(preview);
        for (int i = 0; i < s.size(); i++) drawText(r, s.get(i), PADDING, PADDING + i * LINE);
    }
}
