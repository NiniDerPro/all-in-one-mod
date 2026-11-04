package dev.aio.core.modules.performance;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;

/**
 * XP bündeln: Erfahrungskugeln in der Nähe werden zu einer zusammengefasst (weniger Entities, weniger
 * Lag) und berührte Kugeln ohne die Vanilla-Pause aufgesammelt. Wirkt nur im Einzelspieler (Server-Logik).
 */
public final class XpClumpsModule extends Module {
    public XpClumpsModule() {
        super("xpclumps", "XP Clumps", "Merge XP orbs and pick them up instantly (singleplayer only)", Category.PERFORMANCE, false);
    }

}
