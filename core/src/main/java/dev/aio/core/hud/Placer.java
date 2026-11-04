package dev.aio.core.hud;

import java.util.List;

/**
 * Sucht für ein neu eingeschaltetes HUD-Element einen freien Platz: so nah wie möglich an seiner
 * Standardposition, ohne andere Elemente zu überdecken.
 */
public final class Placer {
    private static final float GAP = 2f;
    private static final float STEP = 2f;

    private Placer() {}

    /** Freie Position (x, y) oder die Wunschposition, wenn nirgends Platz ist. */
    public static float[] findFree(Snapper.Rect wanted, List<Snapper.Rect> others, float screenW, float screenH) {
        if (!overlapsAny(wanted.x(), wanted.y(), wanted.w(), wanted.h(), others)) return new float[]{wanted.x(), wanted.y()};
        float maxX = Math.max(0, screenW - wanted.w()), maxY = Math.max(0, screenH - wanted.h());
        float bestX = wanted.x(), bestY = wanted.y();
        double best = Double.MAX_VALUE;
        // Kandidaten: Raster über den Bildschirm plus direkt neben jedem anderen Element
        for (float y = 0; y <= maxY; y += STEP) {
            for (float x = 0; x <= maxX; x += STEP) {
                double d = dist(x, y, wanted);
                if (d < best && !overlapsAny(x, y, wanted.w(), wanted.h(), others)) {
                    best = d;
                    bestX = x;
                    bestY = y;
                }
            }
        }
        for (Snapper.Rect o : others) {
            float[][] c = {
                    {o.x(), o.y() + o.h() + GAP}, {o.x(), o.y() - wanted.h() - GAP},
                    {o.x() + o.w() + GAP, o.y()}, {o.x() - wanted.w() - GAP, o.y()}};
            for (float[] p : c) {
                float x = Math.max(0, Math.min(maxX, p[0])), y = Math.max(0, Math.min(maxY, p[1]));
                double d = dist(x, y, wanted);
                if (d < best && !overlapsAny(x, y, wanted.w(), wanted.h(), others)) {
                    best = d;
                    bestX = x;
                    bestY = y;
                }
            }
        }
        return new float[]{bestX, bestY};
    }

    private static double dist(float x, float y, Snapper.Rect wanted) {
        double dx = x - wanted.x(), dy = y - wanted.y();
        return dx * dx + dy * dy;
    }

    static boolean overlapsAny(float x, float y, float w, float h, List<Snapper.Rect> others) {
        for (Snapper.Rect o : others) {
            if (x < o.x() + o.w() + GAP && x + w + GAP > o.x() && y < o.y() + o.h() + GAP && y + h + GAP > o.y()) return true;
        }
        return false;
    }
}
