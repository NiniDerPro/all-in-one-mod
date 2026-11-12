package dev.aio.core.modules.performance;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;

/**
 * XP clumping: nearby experience orbs are merged into one (fewer entities, less
 * lag) and touched orbs are picked up without the vanilla pause. Only works in singleplayer (server logic).
 */
public final class XpClumpsModule extends Module {
    public XpClumpsModule() {
        super("xpclumps", "XP Clumps", "Merge XP orbs and pick them up instantly (singleplayer only)", Category.PERFORMANCE, false);
    }

}
