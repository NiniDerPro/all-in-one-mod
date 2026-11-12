package dev.aio.core.modules.client;

import dev.aio.core.compat.CompatMode;
import dev.aio.core.module.Category;
import dev.aio.core.i18n.UiLanguage;
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

/** Global client settings. Always active, cannot be switched off. */
public final class ClientSettingsModule extends Module {
    public final KeybindSetting guiKey;
    public final KeybindSetting hudEditorKey;
    public final EnumSetting<CompatMode> compatMode;
    public final StringSetting allowList;
    public final BooleanSetting toggleMessages;
    public final ColorSetting accent;
    public final BooleanSetting rainbowAccent;
    public final EnumSetting<SortMode> sortMode;
    /** Language of the AIO texts. */
    public final EnumSetting<UiLanguage> language;
    /** Other installed mods with the same function take priority (the AIO module then stays off). */
    public final BooleanSetting preferOtherMods;
    /** "AIO" button in the pause and title menu. */
    public final BooleanSetting menuButton;
    public final BooleanSetting rememberMenu;
    /** Send anonymous usage statistics (can be switched off; the first-start notice explains it). */
    public final BooleanSetting telemetry;
    /** Whether the first-start notice about the statistics was already dismissed (hidden). */
    public final BooleanSetting telemetryNoticeSeen;
    public final BooleanSetting hudHintSeen;

    public ClientSettingsModule() {
        super("client", "Client", "Menu key, server compatibility and look", Category.CLIENT, false);
        language = add(new EnumSetting<>("language", "Language", UiLanguage.AUTO));
        guiKey = add(new KeybindSetting("guiKey", "Menu key", Keys.RIGHT_SHIFT));
        hudEditorKey = add(new KeybindSetting("hudEditorKey", "HUD editor key", Keys.NONE));
        compatMode = add(new EnumSetting<>("compatMode", "Server compatibility", CompatMode.OFF));
        allowList = add(new StringSetting("allowList", "Allowed servers (comma separated)", "", 256));
        allowList.visibleWhen(() -> compatMode.get() == CompatMode.AUTO);
        menuButton = add(new BooleanSetting("menuButton", "AIO button in menus", true));
        rememberMenu = add(new BooleanSetting("rememberMenu", "Reopen where you left off", true));
        telemetry = add(new BooleanSetting("telemetry", "Send anonymous usage statistics", false));
        telemetryNoticeSeen = add(new BooleanSetting("telemetryNoticeSeen", "Stats notice seen", false));
        telemetryNoticeSeen.hidden();
        hudHintSeen = add(new BooleanSetting("hudHintSeen", "HUD hint seen", false));
        hudHintSeen.hidden();
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

    /** Once for the new design: old default (rainbow, purple) becomes the new accent. */
    @Override public void afterLoad() {
        if (styleVersion.intValue() < 2) {
            if ((accent.argb() & 0xFFFFFF) == 0x7C5CFF) accent.reset();
            rainbowAccent.set(false);
            for (var m : core.modules().all()) if (m instanceof HudElement el) el.migrateStyle();
            styleVersion.set(2.0);
        }
    }
}
