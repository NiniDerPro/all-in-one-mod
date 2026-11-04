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
        super("chat", "Chat", "Timestamps, longer history, size and copying", Category.CHAT, false);
        timestamps = add(new BooleanSetting("timestamps", "Timestamps", true));
        seconds = add(new BooleanSetting("seconds", "With seconds", false));
        seconds.visibleWhen(timestamps::on);
        history = add(new NumberSetting("history", "History length", 500, 100, 2000, 50, " lines"));
        copyKey = add(new KeybindSetting("copyKey", "Copy chat key", dev.aio.core.platform.Keys.NONE));
        add(new ActionSetting("copy", "Copy chat", this::openCopy));
        width = vanilla("width", "Width", VanillaOption.CHAT_WIDTH, 1.0);
        heightFocused = vanilla("heightFocused", "Height (open)", VanillaOption.CHAT_HEIGHT_FOCUSED, 1.0);
        heightUnfocused = vanilla("heightUnfocused", "Height (closed)", VanillaOption.CHAT_HEIGHT_UNFOCUSED, 0.4375);
        scale = vanilla("scale", "Text size", VanillaOption.CHAT_SCALE, 1.0);
        lineSpacing = vanilla("lineSpacing", "Line spacing", VanillaOption.CHAT_LINE_SPACING, 0.0);
        textOpacity = vanilla("textOpacity", "Text opacity", VanillaOption.CHAT_OPACITY, 1.0);
        backgroundOpacity = vanilla("bgOpacity", "Background", VanillaOption.TEXT_BACKGROUND_OPACITY, 0.5);
        unicodeFont = add(new BooleanSetting("unicodeFont", "Unicode font", false));
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


    public void openCopy() {
        if (core != null) platform().openScreen(new ChatCopyScreen(core, chatHistory, timestamps.on()));
    }

    public ChatHistory chatHistory() { return chatHistory; }

    // ---- Chat-Box im HUD-Editor ----

    /**
     * Breite der Chatbox in GUI-Pixeln. Wie Vanilla: 40 + 280 · Breite. Die Schriftgröße ändert
     * nur, wie viel Text in eine Zeile passt, nicht die Breite der Box.
     */
    public float boxWidth() { return (float) Math.floor(40 + 280 * width.get()); }

    /** Zeilenhöhe in GUI-Pixeln vor der Schriftgröße (Vanilla: 9 · (1 + Zeilenabstand)). */
    public int lineHeight() { return (int) (9 * (1 + lineSpacing.get())); }

    /** Sichtbare Zeilen im offenen Chat (Vanilla: (20 + 160 · Höhe) / Zeilenhöhe). */
    public int visibleLines() { return Math.max(1, (int) Math.floor(20 + 160 * heightFocused.get() + 1e-6) / Math.max(1, lineHeight())); }

    /** Höhe der offenen Chatbox in GUI-Pixeln (Zeilen · Zeilenhöhe · Schriftgröße). */
    public float boxHeight() { return (float) (visibleLines() * lineHeight() * Math.max(0.1, scale.get())); }

    /** Setzt Breite/Höhe aus Pixeln (beim Ziehen am Rand im HUD-Editor oder im Chat). */
    public void resizeTo(float pxWidth, float pxHeight) {
        double s = Math.max(0.1, scale.get());
        width.set((pxWidth - 40) / 280.0);
        // Die Höhe zählt in ganzen Zeilen (wie Vanilla). Etwas Luft, damit das Runden der
        // Einstellung auf 0,01 keine Zeile verschluckt.
        int lineH = Math.max(1, lineHeight());
        int lines = Math.max(1, Math.round(pxHeight / (float) s / lineH));
        if (lines != visibleLines()) heightFocused.set((lines * lineH - 20 + 0.9) / 160);
    }

    private static final String[] SAMPLE = {
            "<Steve> Hi! Who wants to come to the Nether?",
            "<Alex> Me, wait, let me grab some fire resistance potions",
            "[Server] Welcome back to the server!",
            "<Steve> Meet at the portal at 120 / 64 / -340?",
            "<Alex> Sure, on my way",
            "Steve has made the advancement [Hot Stuff]",
            "<Notch> Have fun you two :)",
            "<Steve> Thanks!",
    };

    /**
     * Zeichnet Beispielnachrichten so, wie Minecraft den Chat mit den aktuellen Einstellungen
     * zeigen würde (Schriftgröße, Breite, Zeilenabstand, Deckkraft), als Referenz im HUD-Editor.
     *
     * @param x      linke Kante der Box
     * @param bottom untere Kante der Box
     */
    public void renderPreview(dev.aio.core.platform.Renderer r, float x, float bottom) {
        float s = (float) Math.max(0.1, scale.get());
        int lineH = lineHeight();
        int innerW = (int) Math.ceil(boxWidth() / s);
        int lines = visibleLines();
        // Vanilla: Text-Deckkraft 0.1..1, Hintergrund nach Einstellung
        int textA = (int) (255 * (textOpacity.get() * 0.9 + 0.1));
        int bgA = (int) (255 * backgroundOpacity.get());
        String prefix = timestamps.on() ? "[" + LocalTime.now().format(seconds.on() ? HMS : HM) + "] " : "";

        // Beispiele in Zeilen umbrechen wie Vanilla (an Wortgrenzen)
        java.util.List<String> wrapped = new java.util.ArrayList<>();
        for (String msg : SAMPLE) wrap(r, prefix + msg, innerW, wrapped);

        r.push();
        r.translate(x, bottom);
        r.scale(s);
        int shown = Math.min(lines, wrapped.size());
        for (int i = 0; i < shown; i++) {
            String line = wrapped.get(wrapped.size() - 1 - i);
            float y2 = -i * lineH, y1 = y2 - lineH;
            if (bgA > 0) r.fill(-2, y1, innerW + 2, y2, bgA << 24);
            if (textA > 3) r.text(line, 0, y1 + (lineH - 8) / 2f, (textA << 24) | 0xFFFFFF, true);
        }
        r.pop();
    }

    private static void wrap(dev.aio.core.platform.Renderer r, String text, int maxW, java.util.List<String> out) {
        String rest = text;
        while (!rest.isEmpty()) {
            if (r.textWidth(rest) <= maxW) { out.add(rest); return; }
            int cut = rest.length();
            while (cut > 1 && r.textWidth(rest.substring(0, cut)) > maxW) cut--;
            int space = rest.lastIndexOf(' ', cut);
            if (space > 0) cut = space;
            out.add(rest.substring(0, cut));
            rest = rest.substring(cut).stripLeading();
        }
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
