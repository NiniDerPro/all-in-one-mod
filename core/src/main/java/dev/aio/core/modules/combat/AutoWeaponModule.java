package dev.aio.core.modules.combat;

import dev.aio.core.event.Events;
import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.platform.GameKey;
import dev.aio.core.setting.BooleanSetting;

/**
 * Wechselt zur besten Waffe, sobald das Fadenkreuz auf einem Lebewesen liegt, also bevor man
 * zuschlägt. Wechsel und Schlag im selben Tick würden mit den Werten des alten Items rechnen
 * (Server aktualisiert Attribute erst im nächsten Tick) und gelten als "Attribute Swapping".
 */
public final class AutoWeaponModule extends Module {
    private static final int RESTORE_AFTER_TICKS = 20;

    private final BooleanSetting preferAxe;
    private final BooleanSetting restore;
    private int previousSlot = -1;
    private int idleTicks;

    public AutoWeaponModule() {
        super("autoweapon", "Auto Weapon", "Picks your best weapon when you aim at a mob", Category.PVP, true);
        preferAxe = add(new BooleanSetting("preferAxe", "Prefer axe", false));
        restore = add(new BooleanSetting("restore", "Switch back afterwards", false));
        on(Events.Tick.class, e -> tick());
    }

    private void tick() {
        if (!platform().inWorld() || platform().isScreenOpen()) return;
        if (platform().crosshairOnLiving()) {
            idleTicks = 0;
            int best = platform().bestWeaponSlot(preferAxe.on());
            int current = platform().selectedSlot();
            if (best >= 0 && best != current) {
                if (previousSlot < 0) previousSlot = current;
                platform().selectSlot(best);
            }
            return;
        }
        if (previousSlot < 0 || platform().isGameKeyDown(GameKey.ATTACK)) return;
        if (++idleTicks >= RESTORE_AFTER_TICKS) {
            if (restore.on()) platform().selectSlot(previousSlot);
            previousSlot = -1;
        }
    }

    @Override protected void onDeactivate() { previousSlot = -1; }
}
