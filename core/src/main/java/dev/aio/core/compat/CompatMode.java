package dev.aio.core.compat;

import dev.aio.core.setting.EnumSetting;

public enum CompatMode implements EnumSetting.Named {
    /** Never block. Only for singleplayer or servers that allow everything. */
    OFF("Off"),
    /** Block on multiplayer servers unless the server is on the allow list. */
    AUTO("Auto"),
    /** Immer sperren. */
    ALWAYS("Always");

    private final String displayName;

    CompatMode(String displayName) { this.displayName = displayName; }

    @Override public String displayName() { return displayName; }
}
