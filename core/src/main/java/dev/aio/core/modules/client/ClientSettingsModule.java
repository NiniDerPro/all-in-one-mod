package dev.aio.core.modules.client;

import dev.aio.core.compat.CompatMode;
import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.platform.Keys;
import dev.aio.core.hud.HudElement;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.ColorSetting;
import dev.aio.core.setting.EnumSetting;
import dev.aio.core.setting.KeybindSetting;
import dev.aio.core.setting.NumberSetting;
import dev.aio.core.setting.StringSetting;
import dev.aio.core.ui.SortMode;
import dev.aio.core.ui.Theme;

/** Globale Client-Einstellungen. Immer aktiv, lassen sich nicht ausschalten. */
public final class ClientSettingsModule extends Module {
    public final KeybindSetting guiKey;
    public final KeybindSetting hudEditorKey;
    public final EnumSetting<CompatMode> compatMode;
    public final StringSetting allowList;
    public final BooleanSetting toggleMessages;
    public final ColorSetting accent;
    public final BooleanSetting rainbowAccent;
    public final EnumSetting<SortMode> sortMode;
    /** Andere installierte Mods mit gleicher Funktion haben Vorrang (das AIO-Modul bleibt dann aus). */
    public final BooleanSetting preferOtherMods;
    /** "AIO"-Knopf im Pause- und Titelmenü. */
    public final BooleanSetting menuButton;

    public ClientSettingsModule() {
        super("client", "Client", "Menu key, server compatibility and look", Category.CLIENT, false);
        guiKey = add(new KeybindSetting("guiKey", "Menu key", Keys.RIGHT_SHIFT));
        hudEditorKey = add(new KeybindSetting("hudEditorKey", "HUD editor key", Keys.NONE));
        compatMode = add(new EnumSetting<>("compatMode", "Server compatibility", CompatMode.OFF));
        allowList = add(new StringSetting("allowList", "Allowed servers (comma separated)", "", 256));
        allowList.visibleWhen(() -> compatMode.get() == CompatMode.AUTO);
        menuButton = add(new BooleanSetting("menuButton", "AIO button in menus", true));
        preferOtherMods = add(new BooleanSetting("preferOtherMods", "Defer to other mods", true));
        toggleMessages = add(new BooleanSetting("toggleMessages", "Message when toggling", true));
        rainbowAccent = add(new BooleanSetting("rainbowAccent", "Rainbow accent", false));
        sortMode = add(new EnumSetting<>("sortMode", "Menu sorting", SortMode.DEFAULT));
        accent = add(new ColorSetting("accent", "Accent color", Theme.DEFAULT_ACCENT));
        accent.visibleWhen(() -> !rainbowAccent.on());
        styleVersion = add(new NumberSetting("styleVersion", "Style version", 0, 0, 100, 1, ""));
        styleVersion.hidden();
    }

    @Override public boolean canToggle() { return false; }

    private final NumberSetting styleVersion;

    /** Einmalig zum neuen Design: alter Standard (Regenbogen, Lila) wird zum neuen Akzent. */
    @Override public void afterLoad() {
        if (styleVersion.intValue() < 2) {
            if ((accent.argb() & 0xFFFFFF) == 0x7C5CFF) accent.reset();
            rainbowAccent.set(false);
            for (var m : core.modules().all()) if (m instanceof HudElement el) el.migrateStyle();
            styleVersion.set(2.0);
        }
    }
}
