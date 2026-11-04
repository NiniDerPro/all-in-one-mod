package dev.aio.core.modules.combat;

import dev.aio.core.event.Events;
import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.platform.GameKey;
import dev.aio.core.setting.BooleanSetting;

/**
 * Wechselt beim Schlagen eines Wesens zur besten Waffe in der Hotbar, bevor der Schlag
 * an den Server geht. Der Treffer (auch ein Krit) zählt dadurch direkt mit der Waffe.
 */
public final class AutoWeaponModule extends Module {
    private final BooleanSetting preferAxe;
    private final BooleanSetting onlyLiving;
    private final BooleanSetting restore;
    private int previousSlot = -1;
    private int idleTicks;

    public AutoWeaponModule() {
        super("autoweapon", "Auto Sword", "Wechselt beim Schlagen zur besten Waffe", Category.COMBAT, true);
        preferAxe = add(new BooleanSetting("preferAxe", "Axt bevorzugen", false));
        onlyLiving = add(new BooleanSetting("onlyLiving", "Nur bei Lebewesen", true));
        restore = add(new BooleanSetting("restore", "Danach zurückwechseln", false));
        on(Events.AttackEntity.class, this::onAttack);
        on(Events.Tick.class, e -> tick());
    }

    private void onAttack(Events.AttackEntity e) {
        if (onlyLiving.on() && !e.living()) return;
        int best = platform().bestWeaponSlot(preferAxe.on());
        int current = platform().selectedSlot();
        idleTicks = 0;
        if (best < 0 || best == current) return;
        if (previousSlot < 0) previousSlot = current;
        platform().selectSlot(best);
    }

    private void tick() {
        if (previousSlot < 0) return;
        if (platform().isGameKeyDown(GameKey.ATTACK)) {
            idleTicks = 0;
            return;
        }
        // eine Sekunde nach dem letzten Schlag zurück
        if (++idleTicks >= 20) {
            if (restore.on()) platform().selectSlot(previousSlot);
            previousSlot = -1;
        }
    }

    @Override protected void onDeactivate() { previousSlot = -1; }
}
