package dev.aio.core.modules.map;

import dev.aio.core.platform.Renderer;

/**
 * Zeichnet die Karte aus Rechtecken. Flach: von oben mit Hangschattierung (Licht von Nordwest).
 * Diorama: schräg wie ein Modell auf dem Tisch – Säulen heben sich nach ihrer Höhe, an Kanten stehen
 * dunklere Wände, Höhenunterschiede werfen Schatten. Von hinten nach vorne gezeichnet, damit Nähere
 * Fernes verdecken.
 */
public final class MapRenderer {
    private MapRenderer() {}

    /** Höchstens so viele Felder pro Frame (Rechtecke sind das Nadelöhr, nicht die Daten). */
    private static final int MAX_CELLS = 9000;

    public static void draw(Renderer r, MapStore s, MapView v, boolean round) {
        float rowH = v.ppb() * (v.diorama() ? MapView.TILT : 1f);
        // Zellgröße: mindestens 1 px und so, dass die Feldzahl begrenzt bleibt
        float minCell = Math.max(1f, (float) Math.sqrt(v.w() * v.h() / MAX_CELLS));
        int stride = Math.max(1, (int) Math.ceil(minCell / v.ppb()));
        float cw = v.ppb() * stride, ch = rowH * stride;
        float lift = v.lift();

        int pad = v.diorama() ? 14 : 1; // höhere Säulen weiter hinten sichtbar machen
        int minX = (int) Math.floor(v.cx() - v.w() / 2f / v.ppb()) - 1;
        int maxX = (int) Math.ceil(v.cx() + v.w() / 2f / v.ppb()) + 1;
        int minZ = (int) Math.floor(v.cz() - v.h() / 2f / rowH) - 1;
        int maxZ = (int) Math.ceil(v.cz() + v.h() / 2f / rowH) + pad;
        int sx0 = Math.floorDiv(minX, stride) * stride, sz0 = Math.floorDiv(minZ, stride) * stride;
        float rad = Math.min(v.w(), v.h()) / 2f, mx = v.x0() + v.w() / 2f, my = v.y0() + v.h() / 2f;

        for (int bz = sz0; bz <= maxZ; bz += stride) {
            for (int bx = sx0; bx <= maxX; bx += stride) {
                int argb = s.rgb(bx, bz);
                if (argb == 0) continue;
                int h = s.height(bx, bz);
                float x = v.sx(bx), y = v.sy(bz, h);
                if (round) {
                    float dx = x + cw / 2f - mx, dy = y + ch / 2f - my;
                    if (dx * dx + dy * dy > rad * rad) continue;
                }
                // Schatten/Licht: höher als der Nachbar im Nordwesten = hell, tiefer = dunkel
                int nw = s.rgb(bx - stride, bz - stride) == 0 ? h : s.height(bx - stride, bz - stride);
                float shade = 1f + Math.max(-8, Math.min(8, h - nw)) * 0.045f + Math.max(-20, Math.min(40, h - v.refH())) * 0.002f;
                fill(r, v, x, y, x + cw + 0.5f, y + ch + 0.5f, shade(argb, shade));
                if (v.diorama()) {
                    int sh = s.rgb(bx, bz + stride) == 0 ? h : s.height(bx, bz + stride);
                    int dh = h - sh;
                    if (dh > 0) {
                        float wall = Math.min(dh * lift, 60f);
                        fill(r, v, x, y + ch, x + cw + 0.5f, y + ch + wall, shade(argb, 0.62f));
                    }
                }
            }
        }
    }

    /** Fläche auf den Kartenrahmen zuschneiden. */
    static void fill(Renderer r, MapView v, float x1, float y1, float x2, float y2, int color) {
        x1 = Math.max(x1, v.x0());
        y1 = Math.max(y1, v.y0());
        x2 = Math.min(x2, v.x0() + v.w());
        y2 = Math.min(y2, v.y0() + v.h());
        if (x2 > x1 && y2 > y1) r.fill(x1, y1, x2, y2, color);
    }

    private static int shade(int argb, float k) {
        int rr = Math.min(255, (int) (((argb >> 16) & 255) * k));
        int gg = Math.min(255, (int) (((argb >> 8) & 255) * k));
        int bb = Math.min(255, (int) ((argb & 255) * k));
        return 0xFF000000 | rr << 16 | gg << 8 | bb;
    }

    /** Spielerpfeil: Punkt mit Blickrichtung. */
    public static void player(Renderer r, MapView v, float px, float py, float yawDeg) {
        double a = Math.toRadians(yawDeg);
        float dx = (float) -Math.sin(a), dz = (float) Math.cos(a);
        for (int i = 1; i <= 4; i++) fillDot(r, v, px + dx * i * 1.5f, py + dz * i * 1.5f, 1.2f, 0xFFFFFFFF);
        fillDot(r, v, px, py, 2.2f, 0xFF000000);
        fillDot(r, v, px, py, 1.5f, 0xFFFFFFFF);
    }

    /** Markierung mit dunklem Rand (Wegpunkte, Radar). */
    public static void dot(Renderer r, MapView v, float px, float py, float size, int color) {
        fillDot(r, v, px, py, size + 1f, 0xFF000000);
        fillDot(r, v, px, py, size, color);
    }

    private static void fillDot(Renderer r, MapView v, float x, float y, float s, int color) {
        fill(r, v, x - s, y - s, x + s, y + s, color);
    }
}
