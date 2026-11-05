package dev.aio.core.hud;

import dev.aio.core.setting.EnumSetting;

public enum ColorMode implements EnumSetting.Named {
    /** Werte in der Textfarbe, Beschriftungen (Buchstaben, Trenner) gedimmt: "120 FPS". */
    CLEAN("Clean"),
    /** Eine feste Farbe. */
    STATIC("Static"),
    /** Regenbogen, der als Welle über den Text läuft. */
    CHROMA("Rainbow"),
    /** Verlauf zwischen zwei Farben, der hin und her wandert. */
    GRADIENT("Gradient");

    private final String displayName;

    ColorMode(String displayName) { this.displayName = displayName; }

    @Override public String displayName() { return displayName; }
}
