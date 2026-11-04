package dev.aio.core.modules.render;

import dev.aio.core.setting.EnumSetting;

public enum ActivationMode implements EnumSetting.Named {
    HOLD("Halten"), TOGGLE("Umschalten");

    private final String displayName;

    ActivationMode(String displayName) { this.displayName = displayName; }

    @Override public String displayName() { return displayName; }
}
