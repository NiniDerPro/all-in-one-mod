package dev.aio.core.hud;

import dev.aio.core.setting.EnumSetting;

public enum ColorMode implements EnumSetting.Named {
    /** Werte in der Textfarbe, Beschriftungen (Buchstaben, Trenner) gedimmt: "120 FPS". */
    CLEAN("Clean"),
    /** Eine feste Farbe. */
    STATIC("Static"),
    /** Rainbow that runs across the text as a wave. */
    CHROMA("Rainbow"),
    /** Gradient between two colours that moves back and forth. */
    GRADIENT("Gradient");

    private final String displayName;

    ColorMode(String displayName) { this.displayName = displayName; }

    @Override public String displayName() { return displayName; }
}
