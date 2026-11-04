package dev.aio.core.ui;

import dev.aio.core.AioCore;

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

    public static int accent(AioCore core) {
        return core.clientSettings().accent.argb() | 0xFF000000;
    }
}
