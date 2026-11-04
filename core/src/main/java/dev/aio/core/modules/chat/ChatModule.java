package dev.aio.core.modules.chat;

import dev.aio.core.event.Events;
import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.platform.VanillaOption;
import dev.aio.core.setting.ActionSetting;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.KeybindSetting;
import dev.aio.core.setting.NumberSetting;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Chat-Verbesserungen: Zeitstempel, längerer Verlauf, Kopieren (Rechtsklick im Chat oder Taste)
 * und Größe/Schrift. Breite und Höhe lassen sich auch im HUD-Editor an der Ecke ziehen.
 */
public final class ChatModule extends Module {
    public static final int VANILLA_HISTORY = 100;
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter HMS = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final BooleanSetting timestamps;
    private final BooleanSetting seconds;
    private final NumberSetting history;
    private final KeybindSetting copyKey;
    private final NumberSetting width, heightFocused, heightUnfocused, scale, lineSpacing, textOpacity, backgroundOpacity;
    private final BooleanSetting unicodeFont;
    private final ChatHistory chatHistory = new ChatHistory(1000);
    private boolean applying;
    /** Ob die Vanilla-Werte schon übernommen wurden (beim Spielstart gibt es sie noch nicht). */
    private boolean synced;

    public ChatModule() {
        super("chat", "Chat", "Zeitstempel und längerer Chatverlauf", Category.CHAT, false);
        timestamps = add(new BooleanSetting("timestamps", "Zeitstempel", true));
        seconds = add(new BooleanSetting("seconds", "Mit Sekunden", false));
        seconds.visibleWhen(timestamps::on);
        history = add(new NumberSetting("history", "Verlaufslänge", 500, 100, 2000, 50, " Zeilen"));
        copyKey = add(new KeybindSetting("copyKey", "Chat-kopieren-Taste", dev.aio.core.platform.Keys.NONE));
        add(new ActionSetting("copy", "Chat kopieren", this::openCopy));
        width = vanilla("width", "Breite", VanillaOption.CHAT_WIDTH, 1.0);
        heightFocused = vanilla("heightFocused", "Höhe (offen)", VanillaOption.CHAT_HEIGHT_FOCUSED, 1.0);
        heightUnfocused = vanilla("heightUnfocused", "Höhe (zu)", VanillaOption.CHAT_HEIGHT_UNFOCUSED, 0.4375);
        scale = vanilla("scale", "Schriftgröße", VanillaOption.CHAT_SCALE, 1.0);
        lineSpacing = vanilla("lineSpacing", "Zeilenabstand", VanillaOption.CHAT_LINE_SPACING, 0.0);
        textOpacity = vanilla("textOpacity", "Textdeckkraft", VanillaOption.CHAT_OPACITY, 1.0);
        backgroundOpacity = vanilla("bgOpacity", "Hintergrund", VanillaOption.TEXT_BACKGROUND_OPACITY, 0.5);
        unicodeFont = add(new BooleanSetting("unicodeFont", "Unicode-Schrift", false));
        unicodeFont.onChange(v -> apply(VanillaOption.FORCE_UNICODE_FONT, v ? 1 : 0));
        on(Events.ChatReceived.class, e -> chatHistory.add(e.text()));
        on(Events.Tick.class, e -> { if (!synced) readFromVanilla(); });
        on(Events.Key.class, e -> {
            if (e.isPress() && !e.screenOpen() && copyKey.matches(e.key())) openCopy();
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
     * Übernimmt die aktuellen Vanilla-Werte in die Einstellungen, damit das Modul nichts
     * überschreibt. Geschrieben wird nur, wenn man im Menü oder Editor etwas ändert.
     */
    private void readFromVanilla() {
        if (Double.isNaN(platform().option(VanillaOption.CHAT_WIDTH))) return; // noch nicht geladen, später erneut
        applying = true;
        try {
            width.set(platform().option(VanillaOption.CHAT_WIDTH));
            heightFocused.set(platform().option(VanillaOption.CHAT_HEIGHT_FOCUSED));
            heightUnfocused.set(platform().option(VanillaOption.CHAT_HEIGHT_UNFOCUSED));
            scale.set(platform().option(VanillaOption.CHAT_SCALE));
            lineSpacing.set(platform().option(VanillaOption.CHAT_LINE_SPACING));
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

    @Override public boolean enabledByDefault() { return true; }

    public void openCopy() {
        if (core != null) platform().openScreen(new ChatCopyScreen(core, chatHistory, timestamps.on()));
    }

    public ChatHistory chatHistory() { return chatHistory; }

    // ---- Chat-Box im HUD-Editor ----

    /** Breite der Chatbox in GUI-Pixeln (wie Vanilla: 40 + 280 · Breite, mal Schriftgröße). */
    public float boxWidth() { return (float) ((40 + 280 * width.get()) * Math.max(0.1, scale.get())); }

    /** Höhe der offenen Chatbox in GUI-Pixeln (wie Vanilla: 20 + 160 · Höhe, mal Schriftgröße). */
    public float boxHeight() { return (float) ((20 + 160 * heightFocused.get()) * Math.max(0.1, scale.get())); }

    /** Setzt Breite/Höhe aus Pixeln (beim Ziehen an der Ecke im HUD-Editor). */
    public void resizeTo(float pxWidth, float pxHeight) {
        double s = Math.max(0.1, scale.get());
        width.set((pxWidth / s - 40) / 280);
        heightFocused.set((pxHeight / s - 20) / 160);
    }

    public NumberSetting scaleSetting() { return scale; }

    /** Chatgröße auf die Vanilla-Standardwerte zurücksetzen (HUD-Editor). */
    public void resetBox() {
        width.reset();
        heightFocused.reset();
        heightUnfocused.reset();
        scale.reset();
    }

    /** Präfix wie {@code [12:34] } oder null, wenn deaktiviert. */
    public String timestampPrefix() {
        if (!isActive() || !timestamps.on()) return null;
        return "[" + LocalTime.now().format(seconds.on() ? HMS : HM) + "] ";
    }

    public int historySize() {
        return isActive() ? history.intValue() : VANILLA_HISTORY;
    }
}
