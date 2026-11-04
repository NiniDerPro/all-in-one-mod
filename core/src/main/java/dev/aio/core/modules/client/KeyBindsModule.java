package dev.aio.core.modules.client;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;

/**
 * Ersetzt die Vanilla-Tastenbelegung (Optionen → Steuerung → Tastenbelegung) durch eine Ansicht
 * mit Suche, Konfliktfilter, freien Tasten und "Alle zurücksetzen". Enthält auch die AIO-Tasten.
 */
public final class KeyBindsModule extends Module {
    public KeyBindsModule() {
        super("keybinds", "Keybind Search", "Controls with search, conflicts and free keys", Category.CLIENT, false);
    }


    /** Für den Adapter: soll die Vanilla-Tastenbelegung ersetzt werden? */
    public boolean replacesVanilla() { return isActive(); }

    public void open() { platform().openScreen(new KeyBindsScreen(core)); }
}
