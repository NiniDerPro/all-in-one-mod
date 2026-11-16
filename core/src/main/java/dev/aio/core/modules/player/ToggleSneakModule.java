package dev.aio.core.modules.player;

import dev.aio.core.event.Events;
import dev.aio.core.hud.TextHudElement;
import dev.aio.core.module.Category;
import dev.aio.core.platform.GameKey;
import dev.aio.core.setting.BooleanSetting;

/**
 * Toggle Sneak: one press of the sneak key switches permanent sneaking on or off.
 * Like the usual toggle sneak mods it holds the vanilla sneak key each tick, and pauses while
 * flying or riding, so you neither sink nor get off your mount.
 */
public final class ToggleSneakModule extends TextHudElement {
    private final BooleanSetting showStatus;
    private final BooleanSetting pauseFlyingRiding;
    private boolean toggled;
    private boolean holding;

    public ToggleSneakModule() {
        super("togglesneak", "Toggle Sneak", "Sneak key toggles permanent sneaking", Category.PVP, false, 0.0, 0.96);
        showStatus = add(new BooleanSetting("showStatus", "Show status", true));
        pauseFlyingRiding = add(new BooleanSetting("pauseFlyingRiding", "Pause while flying or riding", true));
        on(Events.Key.class, e -> {
            if (e.isPress() && !e.screenOpen() && platform().matchesGameKey(GameKey.SNEAK, e.key())) toggled = !toggled;
        });
        on(Events.Tick.class, e -> tick());
        on(Events.ServerLeave.class, e -> toggled = false);
    }

    private void tick() {
        boolean hold = toggled && !(pauseFlyingRiding.on() && platform().playerFlyingOrRiding());
        if (hold) {
            platform().setGameKeyPressed(GameKey.SNEAK, true);
            holding = true;
        } else if (holding) {
            platform().setGameKeyPressed(GameKey.SNEAK, false);
            holding = false;
        }
    }

    @Override protected void onDeactivate() {
        toggled = false;
        if (holding) {
            platform().setGameKeyPressed(GameKey.SNEAK, false);
            holding = false;
        }
    }

    public boolean isToggled() { return toggled && isActive(); }

    @Override protected String getText() {
        return showStatus.on() && toggled ? "[Sneaking (toggled)]" : "";
    }

    @Override protected String previewText() { return "[Sneaking (toggled)]"; }
}
