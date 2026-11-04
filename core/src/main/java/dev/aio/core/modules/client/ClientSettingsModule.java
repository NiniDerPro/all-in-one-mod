package dev.aio.core.modules.client;

import dev.aio.core.compat.CompatMode;
import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.platform.Keys;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.ColorSetting;
import dev.aio.core.setting.EnumSetting;
import dev.aio.core.setting.KeybindSetting;
import dev.aio.core.setting.StringSetting;

/** Globale Client-Einstellungen. Immer aktiv, lassen sich nicht ausschalten. */
public final class ClientSettingsModule extends Module {
    public final KeybindSetting guiKey;
    public final KeybindSetting hudEditorKey;
    public final EnumSetting<CompatMode> compatMode;
    public final StringSetting allowList;
    public final BooleanSetting toggleMessages;
    public final ColorSetting accent;
    public final BooleanSetting rainbowAccent;

    public ClientSettingsModule() {
        super("client", "Client", "Menütaste, Server-Kompatibilität und Design", Category.CLIENT, false);
        guiKey = add(new KeybindSetting("guiKey", "Menü-Taste", Keys.RIGHT_SHIFT));
        hudEditorKey = add(new KeybindSetting("hudEditorKey", "HUD-Editor-Taste", Keys.NONE));
        compatMode = add(new EnumSetting<>("compatMode", "Server-Kompatibilität", CompatMode.AUTO));
        allowList = add(new StringSetting("allowList", "Erlaubte Server (Komma)", "", 256));
        allowList.visibleWhen(() -> compatMode.get() == CompatMode.AUTO);
        toggleMessages = add(new BooleanSetting("toggleMessages", "Meldung beim Umschalten", true));
        rainbowAccent = add(new BooleanSetting("rainbowAccent", "Regenbogen-Akzent", true));
        accent = add(new ColorSetting("accent", "Akzentfarbe", 0xFF7C5CFF));
        accent.visibleWhen(() -> !rainbowAccent.on());
    }

    @Override public boolean canToggle() { return false; }
}
