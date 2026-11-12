package dev.aio.core.ui;

import dev.aio.core.AioCore;
import dev.aio.core.i18n.Lang;
import dev.aio.core.platform.Renderer;
import dev.aio.core.update.UpdateCheck;
import dev.aio.core.util.Browser;

/**
 * Notice at the top of the title screen when a newer AIO version exists: "Download" opens the release page,
 * "Later" hides it until the next start, "Skip this version" hides it until an even newer one appears.
 */
public final class UpdateNotice {
    private UpdateNotice() {}

    private static final int WIDTH = 330, HEIGHT = 48;

    public static boolean shouldShow(AioCore core) {
        return UpdateCheck.pending(core);
    }

    private static float[] box(int screenW) {
        float w = Math.min(WIDTH, screenW - 20);
        float x = (screenW - w) / 2f;
        return new float[]{x, 6, x + w, 6 + HEIGHT};
    }

    /** Download, Later, Skip. */
    private static float[][] buttons(int screenW) {
        float[] b = box(screenW);
        float by = b[3] - 17, bh = 13, gap = 5;
        float[] w = {62, 46, 96};
        float total = w[0] + w[1] + w[2] + 2 * gap;
        float x = (b[0] + b[2]) / 2f - total / 2f;
        float[][] out = new float[3][];
        for (int i = 0; i < 3; i++) {
            out[i] = new float[]{x, by, x + w[i], by + bh};
            x += w[i] + gap;
        }
        return out;
    }

    public static void render(AioCore core, Renderer r, int screenW) {
        if (!shouldShow(core)) return;
        UpdateCheck.Release rel = UpdateCheck.available();
        float[] b = box(screenW);
        int accent = Theme.accent(core);
        Draw.round(r, b[0] - 2, b[1] - 1, b[2] + 2, b[3] + 3, 3, 0x50000000);
        Draw.round(r, b[0], b[1], b[2], b[3], 3, Theme.PANEL);
        Draw.roundOutline(r, b[0], b[1], b[2], b[3], Theme.LINE);
        Draw.round(r, b[0], b[1], b[0] + 3, b[3], 1, accent);
        float cx = (b[0] + b[2]) / 2f;
        r.centeredText(Lang.tr("aio.ui.update.line1", "A new version of AIO ClientSide is available: ") + rel.version(),
                cx, b[1] + 6, Theme.TEXT, true);
        r.centeredText(Lang.tr("aio.ui.update.line2", "Nothing is installed automatically."), cx, b[1] + 17, Theme.TEXT_DIM, true);
        float[][] bt = buttons(screenW);
        button(r, bt[0], accent, Lang.tr("aio.ui.update.download", "Download"), true);
        button(r, bt[1], accent, Lang.tr("aio.ui.update.later", "Later"), false);
        button(r, bt[2], accent, Lang.tr("aio.ui.update.skip", "Skip this version"), false);
    }

    private static void button(Renderer r, float[] bt, int accent, String label, boolean primary) {
        Draw.round(r, bt[0], bt[1], bt[2], bt[3], 2, primary ? accent : Theme.CARD);
        if (!primary) Draw.roundOutline(r, bt[0], bt[1], bt[2], bt[3], Theme.LINE);
        float cx = (bt[0] + bt[2]) / 2f;
        r.text(label, cx - r.textWidth(label) / 2f, bt[1] + 3, primary ? 0xFF0E1015 : Theme.TEXT, false);
    }

    /** Handle a click. Returns true if the click was consumed. */
    public static boolean mouseClicked(AioCore core, double mx, double my, int screenW) {
        if (!shouldShow(core)) return false;
        float[][] bt = buttons(screenW);
        for (int i = 0; i < 3; i++) {
            if (!hit(bt[i], mx, my)) continue;
            UpdateCheck.Release rel = UpdateCheck.available();
            if (i == 0) {
                Browser.open(rel.url());
                UpdateCheck.dismissForSession();
            } else if (i == 1) {
                UpdateCheck.dismissForSession();
            } else {
                core.clientSettings().skippedUpdate.set(rel.version());
                core.config().save();
            }
            return true;
        }
        float[] b = box(screenW);
        return mx >= b[0] && mx <= b[2] && my >= b[1] && my <= b[3];
    }

    private static boolean hit(float[] bt, double mx, double my) {
        return mx >= bt[0] && mx <= bt[2] && my >= bt[1] && my <= bt[3];
    }
}
