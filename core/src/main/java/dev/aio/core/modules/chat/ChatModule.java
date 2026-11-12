package dev.aio.core.modules.chat;

import dev.aio.core.event.Events;
import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.platform.Keys;
import dev.aio.core.platform.VanillaOption;
import dev.aio.core.setting.ActionSetting;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.KeybindSetting;
import dev.aio.core.setting.NumberSetting;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Custom Chat: timestamps, longer history, copy (right click in chat or key),
 * opacity and font. The size belongs to {@link FlexChatModule}.
 */
public final class ChatModule extends Module {
    public static final int VANILLA_HISTORY = 100;
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter HMS = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final BooleanSetting timestamps;
    private final BooleanSetting seconds;
    private final NumberSetting history;
    private final KeybindSetting copyKey;
    private final NumberSetting textOpacity, backgroundOpacity;
    private final BooleanSetting unicodeFont;
    private final ChatHistory chatHistory = new ChatHistory(1000);
    private final BooleanSetting splitMigrated;
    private boolean applying;
    /** Whether the vanilla values were already taken over (they do not exist yet at game start). */
    private boolean synced;

    public ChatModule() {
        super("chat", "Custom Chat", "Timestamps, longer history, copying, opacity and font", Category.QOL, false);
        timestamps = add(new BooleanSetting("timestamps", "Timestamps", true));
        seconds = add(new BooleanSetting("seconds", "With seconds", false));
        seconds.visibleWhen(timestamps::on);
        history = add(new NumberSetting("history", "History length", 500, 100, 2000, 50, " lines"));
        copyKey = add(new KeybindSetting("copyKey", "Copy chat key", Keys.NONE));
        add(new ActionSetting("copy", "Copy chat", this::openCopy));
        textOpacity = vanilla("textOpacity", "Text opacity", VanillaOption.CHAT_OPACITY, 1.0);
        backgroundOpacity = vanilla("bgOpacity", "Background", VanillaOption.TEXT_BACKGROUND_OPACITY, 0.5);
        unicodeFont = add(new BooleanSetting("unicodeFont", "Unicode font", false));
        unicodeFont.onChange(v -> apply(VanillaOption.FORCE_UNICODE_FONT, v ? 1 : 0));
        splitMigrated = add(new BooleanSetting("splitMigrated", "Split migrated", false));
        splitMigrated.hidden();
        on(Events.ChatReceived.class, e -> chatHistory.add(e.text()));
        on(Events.Tick.class, e -> { if (!synced) readFromVanilla(); });
        on(Events.Key.class, e -> {
            if (e.isPress() && !e.screenOpen() && copyKey.triggeredBy(platform(), e.key())) openCopy();
        });
    }

    private NumberSetting vanilla(String id, String name, VanillaOption option, double def) {
        NumberSetting s = add(new NumberSetting(id, name, def, 0, 1, 0.01));
        s.onChange(v -> apply(option, v));
        return s;
    }

    private void apply(VanillaOption option, double value) {
        if (!isActive() || applying || core == null || !synced) return;
        platform().setOption(option, value);
    }

    /**
     * Takes the current vanilla values into the settings so that the module does not
     * overwrite anything. Only written when something is changed in the menu or editor.
     */
    private void readFromVanilla() {
        if (Double.isNaN(platform().option(VanillaOption.CHAT_OPACITY))) return; // not loaded yet, try again later
        applying = true;
        try {
            textOpacity.set(platform().option(VanillaOption.CHAT_OPACITY));
            backgroundOpacity.set(platform().option(VanillaOption.TEXT_BACKGROUND_OPACITY));
            unicodeFont.set(platform().option(VanillaOption.FORCE_UNICODE_FONT) > 0.5);
            synced = true;
        } finally {
            applying = false;
        }
    }

    @Override protected void onActivate() {
        synced = false;
        readFromVanilla();
    }


    /**
     * "Chat" used to be on by default and also contained the size. Since the split (Flex Chat
     * for the size) Custom Chat is off by default: switch it off once.
     */
    @Override public boolean adjustLoadedEnabled(boolean enabled) {
        if (splitMigrated.on()) return enabled;
        splitMigrated.set(true);
        return false;
    }

    public void openCopy() {
        if (core != null) platform().openScreen(new ChatCopyScreen(core, chatHistory, timestamps.on()));
    }

    public ChatHistory chatHistory() { return chatHistory; }

    /** Prefix like {@code [12:34] } or null if disabled. */
    public String timestampPrefix() {
        if (!isActive() || !timestamps.on()) return null;
        return "[" + LocalTime.now().format(seconds.on() ? HMS : HM) + "] ";
    }

    public int historySize() {
        return isActive() ? history.intValue() : VANILLA_HISTORY;
    }
}
