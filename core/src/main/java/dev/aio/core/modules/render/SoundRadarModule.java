package dev.aio.core.modules.render;

import dev.aio.core.event.Events;
import dev.aio.core.i18n.Lang;
import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.platform.Perspective;
import dev.aio.core.platform.Renderer;
import dev.aio.core.platform.Vec3;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.NumberSetting;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Sound Radar: shows where sounds come from, like "Visualize sound effects" in Fortnite.
 * Every sound gets an arc on a ring around the crosshair in the direction it came from; footsteps
 * white, hostile mobs red, chests gold, explosions orange. Close sounds are thicker, old ones fade.
 */
public final class SoundRadarModule extends Module {
    private static final int MAX_ENTRIES = 24;

    private static final int WHITE = 0xFFFFFF, RED = 0xFF5555, GOLD = 0xFFC83C, ORANGE = 0xFF8C2E,
            GREEN = 0x7CFF7C, YELLOW = 0xFFF0A0, GREY = 0xC8C8C8, LIGHT_RED = 0xFF9A9A;

    private final NumberSetting radius, range, duration;
    private final BooleanSetting labels, footsteps, hostile, players, neutral, blocks, other, ignoreOwn;
    private final List<Entry> entries = new ArrayList<>();

    private static final class Entry {
        final String key, label;
        final int color;
        double x, y, z;
        long time;

        Entry(String key, String label, int color, double x, double y, double z, long time) {
            this.key = key;
            this.label = label;
            this.color = color;
            this.x = x;
            this.y = y;
            this.z = z;
            this.time = time;
        }
    }

    public SoundRadarModule() {
        super("soundradar", "Sound Radar", "Shows where sounds come from with arcs around the crosshair, like in Fortnite", Category.VISUALS, false);
        radius = add(new NumberSetting("radius", "Ring size", 60, 30, 150, 5, " px"));
        range = add(new NumberSetting("range", "Range", 24, 4, 48, 1, " blocks"));
        duration = add(new NumberSetting("duration", "Show for", 1.5, 0.5, 4, 0.25, " s"));
        labels = add(new BooleanSetting("labels", "Show subtitle", true));
        footsteps = add(new BooleanSetting("footsteps", "Footsteps", true));
        footsteps.group("sounds");
        hostile = add(new BooleanSetting("hostile", "Hostile mobs", true));
        hostile.group("sounds");
        players = add(new BooleanSetting("players", "Players", true));
        players.group("sounds");
        neutral = add(new BooleanSetting("neutral", "Animals", false));
        neutral.group("sounds");
        blocks = add(new BooleanSetting("blocks", "Blocks (chests, doors, ...)", true));
        blocks.group("sounds");
        other = add(new BooleanSetting("other", "Other sounds", false));
        other.group("sounds");
        ignoreOwn = add(new BooleanSetting("ignoreOwn", "Ignore own sounds", true));
        ignoreOwn.group("sounds");
        on(Events.Sound.class, this::onSound);
        on(Events.RenderHud.class, e -> render(e.renderer));
        on(Events.ServerLeave.class, e -> entries.clear());
    }

    @Override protected void onDeactivate() { entries.clear(); }

    @Override public String groupName(String groupId) {
        return "sounds".equals(groupId) ? Lang.tr("aio.ui.soundradar.group.sounds", "Which sounds") : groupId;
    }

    private void onSound(Events.Sound s) {
        if (!platform().inWorld()) return;
        String id = s.id().toLowerCase(Locale.ROOT), src = s.source();
        int color;
        String label = s.subtitle() == null ? "" : s.subtitle();
        if (id.contains(".step") || id.contains("footstep")) {
            if (!footsteps.on()) return;
            color = "hostile".equals(src) ? LIGHT_RED : WHITE;
            if (label.isEmpty()) label = Lang.tr("aio.ui.soundradar.footsteps", "Footsteps");
        } else if (id.contains("explode") || id.contains("primed") || id.contains("tnt")) {
            color = ORANGE;
        } else if (id.contains("chest") || id.contains("barrel") || id.contains("shulker_box") || id.contains("ender_chest")) {
            if (!blocks.on()) return;
            color = GOLD;
        } else if (id.contains("arrow") || id.contains("crossbow") || id.contains("bow.shoot") || id.contains("trident")) {
            color = RED;
        } else {
            switch (src) {
                case "hostile" -> { if (!hostile.on()) return; color = RED; }
                case "player", "players" -> { if (!players.on()) return; color = WHITE; }
                case "neutral" -> { if (!neutral.on()) return; color = GREEN; }
                case "block", "blocks" -> { if (!blocks.on()) return; color = YELLOW; }
                case "music", "record", "records", "ui", "master", "voice", "ambient", "weather" -> { return; }
                default -> { if (!other.on()) return; color = GREY; }
            }
        }

        Vec3 p = platform().playerPos();
        double dx = s.x() - p.x(), dy = s.y() - (p.y() + 1.0), dz = s.z() - p.z();
        double horiz = Math.sqrt(dx * dx + dz * dz);
        double dist = Math.sqrt(horiz * horiz + dy * dy);
        if (dist > range.get()) return;
        if (ignoreOwn.on() && horiz < 1.2 && Math.abs(dy) < 2.5) return;

        String key = (label.isEmpty() ? id : label) + '|' + Math.round(s.x() / 3) + '|' + Math.round(s.z() / 3);
        long now = System.currentTimeMillis();
        for (Entry e : entries) {
            if (e.key.equals(key)) {
                e.x = s.x();
                e.y = s.y();
                e.z = s.z();
                e.time = now;
                return;
            }
        }
        if (entries.size() >= MAX_ENTRIES) entries.remove(0);
        entries.add(new Entry(key, label, color, s.x(), s.y(), s.z(), now));
    }

    private void render(Renderer r) {
        if (entries.isEmpty() || !platform().inWorld() || platform().isScreenOpen()) return;
        if (platform().perspective() == Perspective.THIRD_PERSON_FRONT) return;
        long now = System.currentTimeMillis();
        long life = Math.round(duration.get() * 1000);
        Vec3 p = platform().playerPos();
        float yaw = platform().playerYaw();
        float cx = r.screenWidth() / 2f, cy = r.screenHeight() / 2f;
        float rad = radius.get().floatValue();
        double maxRange = range.get();

        Iterator<Entry> it = entries.iterator();
        while (it.hasNext()) {
            Entry e = it.next();
            long age = now - e.time;
            if (age > life) {
                it.remove();
                continue;
            }
            double dx = e.x - p.x(), dy = e.y - (p.y() + 1.0), dz = e.z - p.z();
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            // MC yaw: 0 = south (+z), 90 = west (-x); positive relative angle = to the right
            double soundYaw = Math.toDegrees(Math.atan2(-dx, dz));
            double rel = Math.toRadians(wrap(soundYaw - yaw));
            float fade = 1f - (float) age / life;
            float near = (float) Math.max(0, 1 - dist / maxRange);
            int alpha = (int) (255 * Math.min(1, 0.25 + 0.75 * fade));
            int argb = (alpha << 24) | e.color;

            drawArc(r, cx, cy, rad, rel, 14 + 10 * near, 1.5f + 2.5f * near, argb);

            if (labels.on()) {
                String text = e.label;
                if (dy > 2.5) text += " ▲";
                else if (dy < -2.5) text += " ▼";
                if (!text.isBlank()) {
                    float lr = rad + 12;
                    float lx = cx + (float) Math.sin(rel) * lr, ly = cy - (float) Math.cos(rel) * lr - r.fontHeight() / 2f;
                    float tw = r.textWidth(text);
                    float tx = lx - tw / 2f + (float) Math.sin(rel) * tw / 2f;
                    r.fill(tx - 2, ly - 1, tx + tw + 2, ly + r.fontHeight(), (Math.min(alpha, 140) << 24));
                    r.text(text, tx, ly, argb, true);
                }
            }
        }
    }

    /** Arc on the ring: many small squares along the circle (degrees span, thickness in GUI pixels). */
    private static void drawArc(Renderer r, float cx, float cy, float rad, double center, double spanDeg, float thick, int argb) {
        double span = Math.toRadians(spanDeg);
        int steps = Math.max(6, (int) (spanDeg * rad / 60));
        for (int i = 0; i <= steps; i++) {
            double a = center - span / 2 + span * i / steps;
            // thinner towards the ends, like a lens
            float t = thick * (float) (0.45 + 0.55 * Math.sin(Math.PI * i / steps));
            float x = cx + (float) Math.sin(a) * rad, y = cy - (float) Math.cos(a) * rad;
            r.fill(x - t / 2, y - t / 2, x + t / 2, y + t / 2, argb);
        }
    }

    private static double wrap(double deg) {
        double d = deg % 360;
        if (d >= 180) d -= 360;
        if (d < -180) d += 360;
        return d;
    }
}
