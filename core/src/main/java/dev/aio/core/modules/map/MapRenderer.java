package dev.aio.core.modules.map;

import dev.aio.core.platform.Renderer;

/**
 * Backt die Karte in ein Pixelbild (CPU, nur bei Änderung) statt tausende Rechtecke pro Frame zu
 * zeichnen. Flach: von oben mit Hangschattierung. Diorama: schräg, Säulen heben sich nach Höhe,
 * dunklere Wände an Kanten, Schatten an Höhenunterschieden; von hinten nach vorne gemalt.
 */
public final class MapRenderer {
    private MapRenderer() {}

    /** Malt die Karte in {@code buf} (sz×sz ARGB). */
    public static void bake(int[] buf, int sz, MapStore s, MapView v, boolean round) {
        java.util.Arrays.fill(buf, 0);
        float rowH = v.ppb() * (v.diorama() ? MapView.TILT : 1f), lift = v.lift();
        int half = (int) Math.ceil(sz / 2f / v.ppb()) + 1;
        int cx = (int) Math.floor(v.cx()), cz = (int) Math.floor(v.cz());
        int zr = (int) Math.ceil(sz / 2f / rowH) + 1;
        int zEnd = cz + zr + (v.diorama() ? 14 : 0);
        for (int bz = cz - zr; bz <= zEnd; bz++) {
            for (int bx = cx - half; bx <= cx + half; bx++) {
                int argb = s.rgb(bx, bz);
                if (argb == 0) continue;
                int h = s.height(bx, bz);
                float x = v.sx(bx), y = v.sy(bz, h);
                int nw = s.rgb(bx - 1, bz - 1) == 0 ? h : s.height(bx - 1, bz - 1);
                float shade = 1f + Math.max(-8, Math.min(8, h - nw)) * 0.05f + Math.max(-20, Math.min(40, h - v.refH())) * 0.002f;
                rect(buf, sz, x, y, x + v.ppb() + 0.6f, y + rowH + 0.6f, shade(argb, shade));
                if (v.diorama()) {
                    int sh = s.rgb(bx, bz + 1) == 0 ? h : s.height(bx, bz + 1);
                    if (h > sh) rect(buf, sz, x, y + rowH, x + v.ppb() + 0.6f, y + rowH + Math.min((h - sh) * lift, 40f), shade(argb, 0.62f));
                }
            }
        }
        if (round) {
            float c = sz / 2f;
            for (int j = 0; j < sz; j++) {
                for (int i = 0; i < sz; i++) {
                    float dx = i + 0.5f - c, dy = j + 0.5f - c;
                    if (dx * dx + dy * dy > c * c) buf[j * sz + i] = 0;
                }
            }
        }
    }

    private static void rect(int[] buf, int sz, float x1, float y1, float x2, float y2, int color) {
        int a = Math.max(0, (int) x1), b = Math.max(0, (int) y1), c = Math.min(sz, (int) Math.ceil(x2)), d = Math.min(sz, (int) Math.ceil(y2));
        for (int j = b; j < d; j++) {
            for (int i = a; i < c; i++) buf[j * sz + i] = color;
        }
    }

    private static int shade(int argb, float k) {
        int rr = Math.min(255, (int) (((argb >> 16) & 255) * k));
        int gg = Math.min(255, (int) (((argb >> 8) & 255) * k));
        int bb = Math.min(255, (int) ((argb & 255) * k));
        return 0xFF000000 | rr << 16 | gg << 8 | bb;
    }

    /** Spielerpfeil: Punkt mit Blickrichtung. */
    public static void player(Renderer r, float px, float py, float yawDeg) {
        double a = Math.toRadians(yawDeg);
        float dx = (float) -Math.sin(a), dz = (float) Math.cos(a);
        for (int i = 1; i <= 4; i++) r.fill(px + dx * i * 1.5f - 1, py + dz * i * 1.5f - 1, px + dx * i * 1.5f + 1, py + dz * i * 1.5f + 1, 0xFFFFFFFF);
        r.fill(px - 2.2f, py - 2.2f, px + 2.2f, py + 2.2f, 0xFF000000);
        r.fill(px - 1.5f, py - 1.5f, px + 1.5f, py + 1.5f, 0xFFFFFFFF);
    }

    /** Markierung mit dunklem Rand (Wegpunkte, Radar). */
    public static void dot(Renderer r, float px, float py, float size, int color) {
        r.fill(px - size - 1, py - size - 1, px + size + 1, py + size + 1, 0xFF000000);
        r.fill(px - size, py - size, px + size, py + size, color);
    }
}
