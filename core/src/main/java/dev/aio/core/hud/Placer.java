package dev.aio.core.hud;

import java.util.List;

/**
 * Finds a free place for a newly switched-on HUD element: as close as possible to its
 * default position without covering other elements.
 */
public final class Placer {
    private static final float GAP = 2f;
    private static final float STEP = 2f;

    private Placer() {}

    /** Free position (x, y) or the wanted position if there is no room anywhere. */
    public static float[] findFree(Snapper.Rect wanted, List<Snapper.Rect> others, float screenW, float screenH) {
        if (!overlapsAny(wanted.x(), wanted.y(), wanted.w(), wanted.h(), others)) return new float[]{wanted.x(), wanted.y()};
        float maxX = Math.max(0, screenW - wanted.w()), maxY = Math.max(0, screenH - wanted.h());
        float bestX = wanted.x(), bestY = wanted.y();
        double best = Double.MAX_VALUE;
        // Candidates: grid across the screen plus directly next to every other element
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
