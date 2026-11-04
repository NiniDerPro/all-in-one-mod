package dev.aio.core.modules.hud;

import dev.aio.core.hud.TextHudElement;
import dev.aio.core.setting.EnumSetting;

/** Klicks pro Sekunde, getrennt für links und rechts. */
public final class CpsModule extends TextHudElement {
    public enum Mode implements EnumSetting.Named {
        BOTH("Links | Rechts"), LEFT("Nur links"), RIGHT("Nur rechts");

        private final String displayName;

        Mode(String displayName) { this.displayName = displayName; }

        @Override public String displayName() { return displayName; }
    }

    private final EnumSetting<Mode> mode;

    public CpsModule() {
        super("cps", "CPS", "Klicks pro Sekunde (links/rechts)", 0.0, 0.06);
        mode = add(new EnumSetting<>("mode", "Anzeige", Mode.BOTH));
    }

    @Override protected void onActivate() {
        core.clicks().acquire();
        super.onActivate();
    }

    @Override protected void onDeactivate() {
        core.clicks().release();
    }

    @Override protected String getText() {
        return switch (mode.get()) {
            case BOTH -> core.clicks().leftCps() + " | " + core.clicks().rightCps() + " CPS";
            case LEFT -> core.clicks().leftCps() + " CPS";
            case RIGHT -> core.clicks().rightCps() + " CPS";
        };
    }

    @Override protected String previewText() { return "0 | 0 CPS"; }
}
