package dev.aio.core.modules.performance;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;

/**
 * XP sofort aufsammeln: Berührte Erfahrungskugeln werden ohne die Vanilla-Pause zwischen zwei
 * Kugeln aufgenommen, dadurch liegen nie Hunderte herum. Wirkt nur im Einzelspieler (Server-Logik).
 */
public final class XpClumpsModule extends Module {
    public XpClumpsModule() {
        super("xpclumps", "XP sofort sammeln", "Erfahrungskugeln ohne Pause aufsammeln (nur Einzelspieler)", Category.PERFORMANCE, false);
    }

    @Override public boolean enabledByDefault() { return true; }
}
