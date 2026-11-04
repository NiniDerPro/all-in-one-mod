package dev.aio.core.compat;

import dev.aio.core.setting.EnumSetting;

public enum CompatMode implements EnumSetting.Named {
    /** Nie sperren. Nur für Singleplayer oder Server, die alles erlauben. */
    OFF("Aus"),
    /** Auf Multiplayer-Servern sperren, außer der Server steht auf der Erlaubt-Liste. */
    AUTO("Auto"),
    /** Immer sperren. */
    ALWAYS("Immer");

    private final String displayName;

    CompatMode(String displayName) { this.displayName = displayName; }

    @Override public String displayName() { return displayName; }
}
