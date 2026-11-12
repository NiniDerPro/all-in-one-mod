package dev.aio.core.i18n;

import dev.aio.core.setting.EnumSetting;

/**
 * Languages offered in the client settings. {@link #AUTO} follows the game language; the others force
 * a language independent of Minecraft's setting. Display names are written in the language itself.
 * A language only belongs here if {@code assets/aio/lang/<code>.json} exists.
 */
public enum UiLanguage implements EnumSetting.Named {
    AUTO("Auto (game language)", ""),
    ENGLISH("English", "en_us"),
    GERMAN("Deutsch", "de_de"),
    SPANISH("Español", "es_es"),
    FRENCH("Français", "fr_fr");

    private final String displayName;
    private final String code;

    UiLanguage(String displayName, String code) {
        this.displayName = displayName;
        this.code = code;
    }

    @Override public String displayName() { return displayName; }

    /** Language file name without extension, empty for {@link #AUTO}. */
    public String code() { return code; }
}
