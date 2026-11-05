package dev.aio.core.ui;

import dev.aio.core.AioCore;
import dev.aio.core.platform.Renderer;
import dev.aio.core.util.Colors;

/** Farben der Oberfläche. Die Akzentfarbe ist in den Client-Einstellungen wählbar. */
public final class Theme {
    private Theme() {}

    public static final int OVERLAY = 0x90000000;
    public static final int PANEL = 0xF0121218;
    public static final int SIDEBAR = 0xF00C0C10;
    public static final int CARD = 0xFF1C1C24;
    public static final int CARD_HOVER = 0xFF262630;
    public static final int CARD_DISABLED = 0xFF15151B;
    public static final int TEXT = 0xFFFFFFFF;
    public static final int TEXT_DIM = 0xFF9A9AA8;
    public static final int TEXT_MUTED = 0xFF5E5E6A;
    public static final int TRACK = 0xFF34343F;
    public static final int WARNING = 0xFFFFB02E;
    public static final int DANGER = 0xFFE5484D;

    /** Akzentfarbe: fest oder (Standard) ein langsam wandernder Regenbogen. */
    public static int accent(AioCore core) {
        return accent(core, 0);
    }

    /** Akzentfarbe mit Versatz, damit z. B. Leisten als Verlauf erscheinen. */
    public static int accent(AioCore core, float offset) {
        if (core.clientSettings().rainbowAccent.on()) return Colors.chroma(offset, 0.15f, 255);
        return core.clientSettings().accent.argb() | 0xFF000000;
    }

    /** Leiste als Farbverlauf (Regenbogen oder Akzent). */
    public static void accentBar(Renderer r, AioCore core, float x1, float y1, float x2, float y2) {
        if (!core.clientSettings().rainbowAccent.on()) {
            r.fill(x1, y1, x2, y2, accent(core));
            return;
        }
        float step = 4;
        for (float x = x1; x < x2; x += step) {
            r.fill(x, y1, Math.min(x2, x + step), y2, accent(core, x));
        }
    }
}
