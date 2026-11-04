package dev.aio.core.modules.client;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;

/**
 * Ersetzt die Vanilla-Tastenbelegung (Optionen → Steuerung → Tastenbelegung) durch eine Ansicht
 * mit Suche, Konfliktfilter, freien Tasten und "Alle zurücksetzen". Enthält auch die AIO-Tasten.
 */
public final class KeyBindsModule extends Module {
    public KeyBindsModule() {
        super("keybinds", "Tastensuche", "Tastenbelegung mit Suche, Konflikten und freien Tasten", Category.CLIENT, false);
    }

    @Override public boolean enabledByDefault() { return true; }

    /** Für den Adapter: soll die Vanilla-Tastenbelegung ersetzt werden? */
    public boolean replacesVanilla() { return isActive(); }

    public void open() { platform().openScreen(new KeyBindsScreen(core)); }
}
