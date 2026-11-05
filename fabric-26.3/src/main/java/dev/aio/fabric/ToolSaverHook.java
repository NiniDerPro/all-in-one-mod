package dev.aio.fabric;

import dev.aio.core.AioCore;
import dev.aio.core.modules.player.ToolSaverModule;
import dev.aio.core.platform.ItemRef;
import net.minecraft.world.item.ItemStack;

/** Tool Saver: gemeinsame Prüfung für alle Eingriffe. */
public final class ToolSaverHook {
    private ToolSaverHook() {}

    /** true = Benutzung verhindern (und Bescheid geben). */
    public static boolean block(ItemStack stack) {
        AioCore core = AioCore.get();
        ToolSaverModule m = core == null ? null : core.module(ToolSaverModule.class);
        if (m == null || stack == null || stack.isEmpty()) return false;
        ItemRef ref = FabricItemRef.of(stack);
        if (!m.shouldBlock(ref)) return false;
        m.onBlocked(ref);
        return true;
    }
}
