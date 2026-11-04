package dev.aio.core.modules.navigation;

import dev.aio.core.hud.ColorMode;
import dev.aio.core.hud.HudElement;
import dev.aio.core.module.Category;
import dev.aio.core.modules.render.FreelookModule;
import dev.aio.core.platform.Renderer;
import dev.aio.core.platform.Vec3;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.EnumSetting;
import dev.aio.core.setting.NumberSetting;

/** Kompass als Leiste oben (wie in Battle-Royale-Spielen) oder als runde Kompassrose. */
public final class CompassModule extends HudElement {
    public enum Style implements EnumSetting.Named {
        BAR("Bar"), ROUND("Round");

        private final String displayName;

        Style(String displayName) { this.displayName = displayName; }

        @Override public String displayName() { return displayName; }
    }

    private final EnumSetting<Style> style;
    private final NumberSetting barWidth;
    private final NumberSetting fieldOfView;
    private final NumberSetting barHeight;
    private final NumberSetting radius;
    private final BooleanSetting showDegrees;
    private final BooleanSetting showWaypoints;

    public CompassModule() {
        super("compass", "Compass", "Cardinal directions as a bar or circle, with waypoints", Category.QOL, false, 0.5, 0.0);
        style = add(new EnumSetting<>("style", "Style", Style.BAR));
        barWidth = add(new NumberSetting("barWidth", "Width", 220, 100, 400, 10, " px"));
        barWidth.visibleWhen(() -> style.get() == Style.BAR);
        fieldOfView = add(new NumberSetting("fov", "Field of view", 180, 90, 360, 15, "°"));
        fieldOfView.visibleWhen(() -> style.get() == Style.BAR);
        barHeight = add(new NumberSetting("barHeight", "Height", 15, 13, 30, 1, " px"));
        barHeight.visibleWhen(() -> style.get() == Style.BAR);
        radius = add(new NumberSetting("radius", "Radius", 30, 18, 60, 1, " px"));
        radius.visibleWhen(() -> style.get() == Style.ROUND);
        showDegrees = add(new BooleanSetting("degrees", "Show degrees", true));
        showWaypoints = add(new BooleanSetting("waypoints", "Show waypoints", true));
    }

    @Override protected ColorMode defaultColorMode() { return ColorMode.STATIC; }

    /** Aktueller Kurs der Kamera (bei Freelook die Kamerarichtung). */
    private float heading() {
        FreelookModule fl = core.module(FreelookModule.class);
        float yaw = fl != null && fl.isEngaged() ? fl.cameraYaw() : platform().playerYaw();
        return Compass.heading(yaw);
    }

    @Override public int width(Renderer r) {
        return style.get() == Style.BAR ? barWidth.intValue() : radius.intValue() * 2 + PADDING * 2;
    }

    @Override public int height(Renderer r) {
        return style.get() == Style.BAR ? barHeight.intValue() + (showDegrees.on() ? 10 : 0) : radius.intValue() * 2 + PADDING * 2 + (showDegrees.on() ? 10 : 0);
    }

    @Override protected boolean drawsDefaultBackground() { return false; }

    @Override protected void renderContent(Renderer r, boolean preview) {
        if (style.get() == Style.BAR) renderBar(r);
        else renderRound(r);
    }

    private void renderBar(Renderer r) {
        int w = barWidth.intValue();
        int barH = barHeight.intValue();
        // Schrift bleibt immer gleich groß; nur der Platz für die Striche darunter wächst mit der Höhe
        int textY = barH >= 20 ? 3 : 2;
        int tickSpace = Math.max(2, barH - textY - 10);
        if (background.on()) r.fill(0, 0, w, barH, backgroundColor.argb());
        float h = heading();
        float fov = fieldOfView.floatValue();
        float pxPerDeg = w / fov;
        float center = w / 2f;

        int start = (int) Math.floor(h - fov / 2) - 1;
        int end = (int) Math.ceil(h + fov / 2) + 1;
        for (int deg = start - Math.floorMod(start, 5); deg <= end; deg += 5) {
            float x = center + Compass.delta(h, Compass.wrap(deg)) * pxPerDeg;
            if (x < 0 || x > w) continue;
            String label = Compass.label(deg);
            if (label != null) {
                float lw = r.textWidth(label);
                if (x - lw / 2f < 1 || x + lw / 2f > w - 1) continue; // am Rand abgeschnitten: weglassen
                boolean cardinal = label.length() == 1;
                int color = cardinal ? (label.equals("N") ? 0xFFFF5555 : textColor()) : 0xFFBBBBBB;
                if (colorMode() != ColorMode.STATIC) color = dynamicColor(x);
                r.text(label, x - r.textWidth(label) / 2f, textY, color, shadow());
                r.fill(x - 0.5f, barH - Math.min(6, tickSpace) - 1, x + 0.5f, barH - 1, 0xC0FFFFFF);
            } else {
                float len = Math.min(Math.floorMod(deg, 15) == 0 ? 5 : 3, tickSpace + 2);
                r.fill(x - 0.5f, barH - len - 1, x + 0.5f, barH - 1, 0x80FFFFFF);
            }
        }

        if (showWaypoints.on()) {
            WaypointsModule wp = core.module(WaypointsModule.class);
            if (wp != null) {
                Vec3 p = platform().playerPos();
                for (Waypoint wpt : wp.shownHere()) {
                    float b = Compass.bearing(wpt.x() + 0.5 - p.x(), wpt.z() + 0.5 - p.z());
                    float d = Compass.delta(h, b);
                    float x = Math.max(3, Math.min(w - 3, center + Math.max(-fov / 2, Math.min(fov / 2, d)) * pxPerDeg));
                    int c = wpt.color() | 0xFF000000;
                    float my = barH - 4;
                    r.fill(x - 2, my - 2, x + 2, my + 2, c);
                    r.fill(x - 1, my - 3, x + 1, my + 3, c);
                }
            }
        }
        // Mittelmarke
        r.fill(center - 0.5f, 0, center + 0.5f, barH, 0xFFFFFFFF);
        r.fill(center - 2, 0, center + 2, 1, 0xFFFFFFFF);
        if (showDegrees.on()) {
            String deg = Math.round(h) % 360 + "°";
            r.text(deg, center - r.textWidth(deg) / 2f, barH + 2, textColor(), shadow());
        }
    }

    private void renderRound(Renderer r) {
        int rad = radius.intValue();
        float cx = PADDING + rad, cy = PADDING + rad;
        float h = heading();
        // Hintergrundscheibe aus Zeilen
        if (background.on()) {
            for (int dy = -rad; dy <= rad; dy++) {
                float half = (float) Math.sqrt(rad * rad - dy * dy);
                r.fill(cx - half, cy + dy, cx + half, cy + dy + 1, backgroundColor.argb());
            }
        }
        // Ring
        for (int a = 0; a < 360; a += 5) {
            double rad2 = Math.toRadians(a - h);
            float x = cx + (float) Math.sin(rad2) * rad;
            float y = cy - (float) Math.cos(rad2) * rad;
            float s = a % 45 == 0 ? 1.5f : 0.5f;
            r.fill(x - s, y - s, x + s, y + s, 0xA0FFFFFF);
        }
        // Richtungen (drehen sich mit, oben = Blickrichtung)
        for (int a = 0; a < 360; a += 45) {
            String label = Compass.label(a);
            double ang = Math.toRadians(a - h);
            float lr = label.length() == 1 ? rad - 9 : rad - 11;
            float x = cx + (float) Math.sin(ang) * lr;
            float y = cy - (float) Math.cos(ang) * lr;
            int color = label.equals("N") ? 0xFFFF5555 : label.length() == 1 ? textColor() : 0xFFAAAAAA;
            if (colorMode() != ColorMode.STATIC && !label.equals("N")) color = dynamicColor(a);
            if (label.length() == 2) {
                r.push();
                r.translate(x, y);
                r.scale(0.7f);
                r.text(label, -r.textWidth(label) / 2f, -4, color, shadow());
                r.pop();
            } else {
                r.text(label, x - r.textWidth(label) / 2f, y - 4, color, shadow());
            }
        }
        // Wegpunkte als Punkte am Rand
        if (showWaypoints.on()) {
            WaypointsModule wp = core.module(WaypointsModule.class);
            if (wp != null) {
                Vec3 p = platform().playerPos();
                for (Waypoint wpt : wp.shownHere()) {
                    double ang = Math.toRadians(Compass.bearing(wpt.x() + 0.5 - p.x(), wpt.z() + 0.5 - p.z()) - h);
                    float x = cx + (float) Math.sin(ang) * (rad - 2);
                    float y = cy - (float) Math.cos(ang) * (rad - 2);
                    r.fill(x - 2, y - 2, x + 2, y + 2, wpt.color() | 0xFF000000);
                }
            }
        }
        // Nadel (Blickrichtung zeigt immer nach oben)
        r.fill(cx - 0.5f, cy - rad + 3, cx + 0.5f, cy, 0xFFFFFFFF);
        r.fill(cx - 1, cy - 1, cx + 1, cy + 1, 0xFFFFFFFF);
        if (showDegrees.on()) {
            String deg = Math.round(h) % 360 + "°";
            r.text(deg, cx - r.textWidth(deg) / 2f, PADDING * 2 + rad * 2 + 1, textColor(), shadow());
        }
    }
}
