package dev.aio.core.ui;

import dev.aio.core.AioCore;
import dev.aio.core.platform.Renderer;
import dev.aio.core.util.Colors;

/**
 * Colours of the interface: dark graphite with a slightly bluish tint, text in three shades,
 * one accent colour (selectable in the client settings) only for active things and focus.
 */
public final class Theme {
    private Theme() {}

    public static final int OVERLAY = 0xA0050608;
    /** Areas from back to front. */
    public static final int PANEL = 0xF20E1015;
    public static final int SIDEBAR = 0xF2090A0E;
    public static final int SURFACE = 0xFF151820;
    public static final int CARD = 0xFF181B24;
    public static final int CARD_HOVER = 0xFF20242F;
    public static final int CARD_DISABLED = 0xFF12141A;
    /** Fine lines and borders. */
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

    /** Accent colour; with "Rainbow accent" a slowly moving rainbow (offset for gradients). */
    public static int accent(AioCore core, float offset) {
        if (core.clientSettings().rainbowAccent.on()) return Colors.chroma(offset, 0.15f, 255);
        return core.clientSettings().accent.argb() | 0xFF000000;
    }

    /** Thin bar in the accent colour (as a gradient with rainbow). */
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
