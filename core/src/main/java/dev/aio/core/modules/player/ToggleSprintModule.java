package dev.aio.core.modules.player;

import dev.aio.core.event.Events;
import dev.aio.core.hud.TextHudElement;
import dev.aio.core.module.Category;
import dev.aio.core.platform.GameKey;
import dev.aio.core.setting.BooleanSetting;

/**
 * Toggle Sprint: one press of the sprint key switches permanent sprinting on or off.
 * Corresponds to the vanilla option "Sprint: Toggle", only with a status display.
 */
public final class ToggleSprintModule extends TextHudElement {
    private final BooleanSetting showStatus;
    private boolean toggled;
    private boolean holding;

    public ToggleSprintModule() {
        super("togglesprint", "Toggle Sprint", "Sprint key toggles permanent sprinting", Category.PVP, true, 0.0, 1.0);
        showStatus = add(new BooleanSetting("showStatus", "Show status", true));
        on(Events.Key.class, e -> {
            if (e.isPress() && !e.screenOpen() && platform().matchesGameKey(GameKey.SPRINT, e.key())) toggled = !toggled;
        });
        on(Events.Tick.class, e -> tick());
        on(Events.ServerLeave.class, e -> toggled = false);
    }

    private void tick() {
        if (toggled) {
            platform().setGameKeyPressed(GameKey.SPRINT, true);
            holding = true;
        } else if (holding) {
            platform().setGameKeyPressed(GameKey.SPRINT, false);
            holding = false;
        }
    }

    @Override protected void onDeactivate() {
        toggled = false;
        if (holding) {
            platform().setGameKeyPressed(GameKey.SPRINT, false);
            holding = false;
        }
    }

    public boolean isToggled() { return toggled && isActive(); }

    @Override protected String getText() {
        return showStatus.on() && toggled ? "[Sprinting (toggled)]" : "";
    }

    @Override protected String previewText() { return "[Sprinting (toggled)]"; }
}
