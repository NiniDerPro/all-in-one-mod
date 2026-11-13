package dev.aio.forge;

import dev.aio.core.AioCore;
import dev.aio.core.i18n.Lang;
import dev.aio.core.platform.Renderer;
import dev.aio.core.ui.Draw;
import dev.aio.core.ui.Theme;

/**
 * One-time notice at the top of the title screen that AIO can send anonymous usage statistics.
 * The statistics are OFF by default; "Okay" switches them on, "No thanks" keeps them off. The notice never comes back afterwards.
 */
final class StatsNotice {
    private StatsNotice() {}

    private static final int WIDTH = 330, HEIGHT = 48;

    static boolean shouldShow(AioCore core) {
        return !core.clientSettings().telemetryNoticeSeen.on();
    }

    private static float[] box(int screenW) {
        float w = Math.min(WIDTH, screenW - 20);
        float x = (screenW - w) / 2f;
        return new float[]{x, 6, x + w, 6 + HEIGHT};
    }

    /** x1,y1,x2,y2 of the accept button and the decline button. */
    private static float[][] buttons(int screenW) {
        float[] b = box(screenW);
        float by = b[3] - 17, bh = 13;
        float aioW = 60, okW = 70, gap = 6;
        float total = aioW + gap + okW;
        float bx = (b[0] + b[2]) / 2f - total / 2f;
        return new float[][]{
                {bx, by, bx + aioW, by + bh},
                {bx + aioW + gap, by, bx + aioW + gap + okW, by + bh}};
    }

    static void render(AioCore core, Renderer r, int screenW) {
        if (!shouldShow(core)) return;
        float[] b = box(screenW);
        int accent = Theme.accent(core);
        Draw.round(r, b[0] - 2, b[1] - 1, b[2] + 2, b[3] + 3, 3, 0x50000000);
        Draw.round(r, b[0], b[1], b[2], b[3], 3, Theme.PANEL);
        Draw.roundOutline(r, b[0], b[1], b[2], b[3], Theme.LINE);
        Draw.round(r, b[0], b[1], b[0] + 3, b[3], 1, accent);
        float cx = (b[0] + b[2]) / 2f;
        r.centeredText(Lang.tr("aio.ui.stats_notice.line1", "Share anonymous usage data for public statistics?"),
                cx, b[1] + 6, Theme.TEXT, true);
        r.centeredText(Lang.tr("aio.ui.stats_notice.line2", "No names or IPs. You can change this anytime in the settings."),
                cx, b[1] + 17, Theme.TEXT_DIM, true);

        float[][] bt = buttons(screenW);
        button(r, bt[0], accent, Lang.tr("aio.ui.stats_notice.accept", "Okay"), true);
        button(r, bt[1], accent, Lang.tr("aio.ui.stats_notice.decline", "No thanks"), false);
    }

    private static void button(Renderer r, float[] bt, int accent, String label, boolean primary) {
        Draw.round(r, bt[0], bt[1], bt[2], bt[3], 2, primary ? accent : Theme.CARD);
        if (!primary) Draw.roundOutline(r, bt[0], bt[1], bt[2], bt[3], Theme.LINE);
        float cx = (bt[0] + bt[2]) / 2f;
        r.text(label, cx - r.textWidth(label) / 2f, bt[1] + 3, primary ? 0xFF0E1015 : Theme.TEXT, false);
    }

    /** Handle a click. Returns true if the click was consumed. */
    static boolean mouseClicked(AioCore core, double mx, double my, int screenW) {
        if (!shouldShow(core)) return false;
        float[][] bt = buttons(screenW);
        boolean inAio = hit(bt[0], mx, my), inOk = hit(bt[1], mx, my);
        if (!inAio && !inOk) {
            float[] b = box(screenW);
            return mx >= b[0] && mx <= b[2] && my >= b[1] && my <= b[3]; // do not pass clicks in the banner through
        }
        core.clientSettings().telemetryNoticeSeen.set(true);
        core.clientSettings().telemetry.set(inAio); // Okay = on, No thanks = off
        core.config().save();
        return true;
    }

    private static boolean hit(float[] bt, double mx, double my) {
        return mx >= bt[0] && mx <= bt[2] && my >= bt[1] && my <= bt[3];
    }
}
