package dev.aio.core.ui;

import dev.aio.core.AioCore;
import dev.aio.core.platform.Renderer;
import dev.aio.core.util.Colors;

/**
 * Farben der Oberfläche: dunkles Graphit mit leicht bläulichem Stich, Text in drei Abstufungen,
 * eine Akzentfarbe (in den Client-Einstellungen wählbar) nur für Aktives und Fokus.
 */
public final class Theme {
    private Theme() {}

    public static final int OVERLAY = 0xA0050608;
    /** Flächen von hinten nach vorne. */
    public static final int PANEL = 0xF20E1015;
    public static final int SIDEBAR = 0xF2090A0E;
    public static final int SURFACE = 0xFF151820;
    public static final int CARD = 0xFF181B24;
    public static final int CARD_HOVER = 0xFF20242F;
    public static final int CARD_DISABLED = 0xFF12141A;
    /** Feine Linien und Ränder. */
    public static final int LINE = 0x14FFFFFF;
    public static final int LINE_STRONG = 0x26FFFFFF;
    public static final int TEXT = 0xFFECEEF3;
    public static final int TEXT_DIM = 0xFF959BAD;
    public static final int TEXT_MUTED = 0xFF5D6375;
    public static final int TRACK = 0xFF2A2F3B;
    public static final int WARNING = 0xFFFFB547;
    public static final int DANGER = 0xFFFF5C5C;
    public static final int SUCCESS = 0xFF4ADE80;

    /** Standard-Akzent: warmes Orange. */
    public static final int DEFAULT_ACCENT = 0xFFFF7A3D;

    public static int accent(AioCore core) {
        return accent(core, 0);
    }

    /** Akzentfarbe; mit "Rainbow accent" ein langsam wandernder Regenbogen (Versatz für Verläufe). */
    public static int accent(AioCore core, float offset) {
        if (core.clientSettings().rainbowAccent.on()) return Colors.chroma(offset, 0.15f, 255);
        return core.clientSettings().accent.argb() | 0xFF000000;
    }

    /** Dünne Leiste in der Akzentfarbe (bei Regenbogen als Verlauf). */
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
