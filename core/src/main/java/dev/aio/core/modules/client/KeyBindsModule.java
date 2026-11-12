package dev.aio.core.modules.client;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;

/**
 * Replaces the vanilla key binding screen (Options → Controls → Key Binds) with a view
 * with search, conflict filter, free keys and "Reset all". Also contains the AIO keys.
 */
public final class KeyBindsModule extends Module {
    public KeyBindsModule() {
        super("keybinds", "Keybind Search", "Controls with search, conflicts and free keys", Category.CLIENT, false);
    }


    /** For the adapter: should the vanilla key binding screen be replaced? */
    public boolean replacesVanilla() { return isActive(); }

    public void open() { platform().openScreen(new KeyBindsScreen(core)); }
}
