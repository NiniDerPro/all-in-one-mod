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
    public final EnumSetting<dev.aio.core.ui.SortMode> sortMode;

    public ClientSettingsModule() {
        super("client", "Client", "Menu key, server compatibility and look", Category.CLIENT, false);
        guiKey = add(new KeybindSetting("guiKey", "Menu key", Keys.RIGHT_SHIFT));
        hudEditorKey = add(new KeybindSetting("hudEditorKey", "HUD editor key", Keys.NONE));
        compatMode = add(new EnumSetting<>("compatMode", "Server compatibility", CompatMode.OFF));
        allowList = add(new StringSetting("allowList", "Allowed servers (comma separated)", "", 256));
        allowList.visibleWhen(() -> compatMode.get() == CompatMode.AUTO);
        toggleMessages = add(new BooleanSetting("toggleMessages", "Message when toggling", true));
        rainbowAccent = add(new BooleanSetting("rainbowAccent", "Rainbow accent", true));
        sortMode = add(new EnumSetting<>("sortMode", "Menu sorting", dev.aio.core.ui.SortMode.DEFAULT));
        accent = add(new ColorSetting("accent", "Accent color", 0xFF7C5CFF));
        accent.visibleWhen(() -> !rainbowAccent.on());
    }

    @Override public boolean canToggle() { return false; }
}
