package dev.aio.core.modules.chat;

import dev.aio.core.event.Events;
import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.platform.Renderer;
import dev.aio.core.platform.VanillaOption;
import dev.aio.core.setting.NumberSetting;

import java.util.ArrayList;
import java.util.List;

/**
 * Flex Chat: drag the chat size freely, in the open chat (T) at the top/right edge or in the
 * HUD editor. The values are the vanilla options (width, height, text size, line spacing), the
 * module takes them over at start and only writes what you change.
 */
public final class FlexChatModule extends Module {
    private final NumberSetting width, heightFocused, heightUnfocused, scale, lineSpacing;
    private boolean applying;
    /** Whether the vanilla values were already taken over (they do not exist yet at game start). */
    private boolean synced;

    public FlexChatModule() {
        super("flexchat", "Flex Chat", "Drag the chat's edges to resize it, in chat or in the HUD editor", Category.QOL, false);
        width = vanilla("width", "Width", VanillaOption.CHAT_WIDTH, 1.0);
        heightFocused = vanilla("heightFocused", "Height (open)", VanillaOption.CHAT_HEIGHT_FOCUSED, 1.0);
        heightUnfocused = vanilla("heightUnfocused", "Height (closed)", VanillaOption.CHAT_HEIGHT_UNFOCUSED, 0.4375);
        scale = vanilla("scale", "Text size", VanillaOption.CHAT_SCALE, 1.0);
        lineSpacing = vanilla("lineSpacing", "Line spacing", VanillaOption.CHAT_LINE_SPACING, 0.0);
        on(Events.Tick.class, e -> { if (!synced) readFromVanilla(); });
    }

    private NumberSetting vanilla(String id, String name, VanillaOption option, double def) {
        NumberSetting s = add(new NumberSetting(id, name, def, 0, 1, 0.01));
        s.onChange(v -> {
            if (isActive() && !applying && core != null && synced) platform().setOption(option, v);
        });
        return s;
    }

    /** Take over the current vanilla values so that the module does not overwrite anything. */
    private void readFromVanilla() {
        if (Double.isNaN(platform().option(VanillaOption.CHAT_WIDTH))) return; // not loaded yet, try again later
        applying = true;
        try {
            width.set(platform().option(VanillaOption.CHAT_WIDTH));
            heightFocused.set(platform().option(VanillaOption.CHAT_HEIGHT_FOCUSED));
            heightUnfocused.set(platform().option(VanillaOption.CHAT_HEIGHT_UNFOCUSED));
            scale.set(platform().option(VanillaOption.CHAT_SCALE));
            lineSpacing.set(platform().option(VanillaOption.CHAT_LINE_SPACING));
            synced = true;
        } finally {
            applying = false;
        }
    }

    @Override protected void onActivate() {
        synced = false;
        readFromVanilla();
    }

    // ---- Dimensions like vanilla ----

    /** Breite der Chatbox in GUI-Pixeln (Vanilla: 40 + 280 · Breite). */
    public float boxWidth() { return (float) Math.floor(40 + 280 * width.get()); }

    /** Line height in GUI pixels before the font size (vanilla: 9 · (1 + line spacing)). */
    public int lineHeight() { return (int) (9 * (1 + lineSpacing.get())); }

    /** Visible lines in the open chat (vanilla: (20 + 160 · height) / line height). */
    public int visibleLines() { return Math.max(1, (int) Math.floor(20 + 160 * heightFocused.get() + 1e-6) / Math.max(1, lineHeight())); }

    /** Height of the open chat box in GUI pixels (lines · line height · font size). */
    public float boxHeight() { return (float) (visibleLines() * lineHeight() * Math.max(0.1, scale.get())); }

    /** Sets width/height from pixels (when dragging at the edge in the HUD editor or in chat). */
    public void resizeTo(float pxWidth, float pxHeight) {
        double s = Math.max(0.1, scale.get());
        width.set((pxWidth - 40) / 280.0);
        // The height counts in whole lines (like vanilla); some slack against rounding to 0.01.
        int lineH = Math.max(1, lineHeight());
        int lines = Math.max(1, Math.round(pxHeight / (float) s / lineH));
        if (lines != visibleLines()) heightFocused.set((lines * lineH - 20 + 0.9) / 160);
    }

    public NumberSetting scaleSetting() { return scale; }

    /** Reset the chat size to the vanilla default values (HUD editor). */
    public void resetBox() {
        width.reset();
        heightFocused.reset();
        heightUnfocused.reset();
        scale.reset();
    }

    // ---- Vorschau im HUD-Editor ----

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

    public void renderPreview(Renderer r, float x, float bottom) { renderPreview(r, x, bottom, 1f); }

    /**
     * Sample messages as Minecraft would show the chat with the current settings
     * (font size, width, line spacing, opacity); {@code fade} additionally fades it out.
     */
    public void renderPreview(Renderer r, float x, float bottom, float fade) {
        float s = (float) Math.max(0.1, scale.get());
        int lineH = lineHeight();
        int innerW = (int) Math.ceil(boxWidth() / s);
        int lines = visibleLines();
        double textOpacity = option(VanillaOption.CHAT_OPACITY, 1.0);
        double bgOpacity = option(VanillaOption.TEXT_BACKGROUND_OPACITY, 0.5);
        // Vanilla: Text-Deckkraft 0.1..1, Hintergrund nach Einstellung
        int textA = (int) (255 * (textOpacity * 0.9 + 0.1) * fade);
        int bgA = (int) (255 * bgOpacity * fade);
        ChatModule custom = core == null ? null : core.module(ChatModule.class);
        String prefix = custom == null || custom.timestampPrefix() == null ? "" : custom.timestampPrefix();

        List<String> wrapped = new ArrayList<>();
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

    private double option(VanillaOption o, double fallback) {
        double v = core == null ? Double.NaN : platform().option(o);
        return Double.isNaN(v) ? fallback : v;
    }

    /** Text an Wortgrenzen umbrechen wie Vanilla. */
    private static void wrap(Renderer r, String text, int maxW, List<String> out) {
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
}
