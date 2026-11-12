package dev.aio.core.hud;

import java.util.List;

/**
 * Snapping while moving in the HUD editor: edges and centres of the dragged element snap to
 * screen edges, the screen centre and edges/centres of other elements.
 */
public final class Snapper {
    public record Rect(float x, float y, float w, float h) {
        float right() { return x + w; }
        float bottom() { return y + h; }
        float cx() { return x + w / 2f; }
        float cy() { return y + h / 2f; }
    }

    /** Result: new position and optionally the guide lines (NaN = none). */
    public record Result(float x, float y, float guideX, float guideY) {
        public boolean hasGuideX() { return !Float.isNaN(guideX); }
        public boolean hasGuideY() { return !Float.isNaN(guideY); }
    }

    private Snapper() {}

    public static Result snap(Rect moving, List<Rect> others, float screenW, float screenH, float threshold) {
        // Kandidatenlinien
        int n = others.size();
        float[] xs = new float[3 + n * 3];
        float[] ys = new float[3 + n * 3];
        xs[0] = 0; xs[1] = screenW / 2f; xs[2] = screenW;
        ys[0] = 0; ys[1] = screenH / 2f; ys[2] = screenH;
        for (int i = 0; i < n; i++) {
            Rect o = others.get(i);
            xs[3 + i * 3] = o.x; xs[4 + i * 3] = o.cx(); xs[5 + i * 3] = o.right();
            ys[3 + i * 3] = o.y; ys[4 + i * 3] = o.cy(); ys[5 + i * 3] = o.bottom();
        }

        float[] myX = {moving.x, moving.cx(), moving.right()};
        float[] myY = {moving.y, moving.cy(), moving.bottom()};

        float bestDx = Float.NaN, guideX = Float.NaN;
        for (float line : xs) {
            for (float edge : myX) {
                float d = line - edge;
                if (Math.abs(d) <= threshold && (Float.isNaN(bestDx) || Math.abs(d) < Math.abs(bestDx))) {
                    bestDx = d;
                    guideX = line;
                }
            }
        }
        float bestDy = Float.NaN, guideY = Float.NaN;
        for (float line : ys) {
            for (float edge : myY) {
                float d = line - edge;
                if (Math.abs(d) <= threshold && (Float.isNaN(bestDy) || Math.abs(d) < Math.abs(bestDy))) {
                    bestDy = d;
                    guideY = line;
                }
            }
        }

        float x = moving.x + (Float.isNaN(bestDx) ? 0 : bestDx);
        float y = moving.y + (Float.isNaN(bestDy) ? 0 : bestDy);
        // Im Bildschirm halten
        x = Math.max(0, Math.min(screenW - moving.w, x));
        y = Math.max(0, Math.min(screenH - moving.h, y));
        return new Result(x, y, guideX, guideY);
    }
}
