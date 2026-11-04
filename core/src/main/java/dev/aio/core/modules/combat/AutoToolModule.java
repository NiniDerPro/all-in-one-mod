package dev.aio.core.modules.combat;

import dev.aio.core.event.Events;
import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.platform.GameKey;
import dev.aio.core.setting.BooleanSetting;

/**
 * Wechselt beim Abbauen automatisch zum besten Werkzeug in der Hotbar (Spitzhacke für Stein,
 * Schaufel für Erde, ...) und auf Wunsch danach zurück.
 */
public final class AutoToolModule extends Module {
    private final BooleanSetting restore;
    private int previousSlot = -1;

    public AutoToolModule() {
        super("autotool", "Auto Tool", "Wechselt beim Abbauen zum passenden Werkzeug", Category.COMBAT, true);
        restore = add(new BooleanSetting("restore", "Danach zurückwechseln", false));
        on(Events.StartBreak.class, e -> onBreak());
        on(Events.Tick.class, e -> tick());
    }

    private void onBreak() {
        int best = platform().bestToolSlot();
        int current = platform().selectedSlot();
        if (best < 0 || best == current) return;
        if (previousSlot < 0) previousSlot = current;
        platform().selectSlot(best);
    }

    private void tick() {
        if (previousSlot >= 0 && !platform().isGameKeyDown(GameKey.ATTACK)) {
            if (restore.on()) platform().selectSlot(previousSlot);
            previousSlot = -1;
        }
    }

    @Override protected void onDeactivate() { previousSlot = -1; }
}
