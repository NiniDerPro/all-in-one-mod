package dev.aio.forge;

import dev.aio.core.platform.VanillaOption;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;

/** Reading and setting individual vanilla options (chat size, sensitivity, …), saved collectively. */
final class VanillaOptions {
    private final Minecraft client;

    VanillaOptions(Minecraft client) {
        this.client = client;
    }

    private long optionsDirtyAt = -1;

    @SuppressWarnings("unchecked")
    private OptionInstance<?> optionInstance(VanillaOption o) {
        Options op = client.options;
        return switch (o) {
            case CHAT_WIDTH -> op.chatWidth();
            case CHAT_HEIGHT_FOCUSED -> op.chatHeightFocused();
            case CHAT_HEIGHT_UNFOCUSED -> op.chatHeightUnfocused();
            case CHAT_SCALE -> op.chatScale();
            case CHAT_LINE_SPACING -> op.chatLineSpacing();
            case CHAT_OPACITY -> op.chatOpacity();
            case TEXT_BACKGROUND_OPACITY -> op.textBackgroundOpacity();
            case FORCE_UNICODE_FONT -> op.forceUnicodeFont();
            case MOUSE_SENSITIVITY -> op.sensitivity();
        };
    }

    double get(VanillaOption o) {
        if (client.options == null) return Double.NaN; // game start: options not loaded yet
        Object v = optionInstance(o).get();
        if (v instanceof Boolean b) return b ? 1 : 0;
        return v instanceof Number n ? n.doubleValue() : 0;
    }

    @SuppressWarnings("unchecked")
    void set(VanillaOption o, double value) {
        if (client.options == null) return;
        if (o == VanillaOption.FORCE_UNICODE_FONT) {
            OptionInstance<Boolean> opt = (OptionInstance<Boolean>) optionInstance(o);
            boolean b = value > 0.5;
            if (opt.get() != b) opt.set(b);
        } else {
            ((OptionInstance<Double>) optionInstance(o)).set(Math.max(0, Math.min(1, value)));
        }
        optionsDirtyAt = System.currentTimeMillis();
    }

    /** Called by the tick: save options collectively (not on every drag step). */
    void saveIfDirty() {
        if (client.options != null && optionsDirtyAt > 0 && System.currentTimeMillis() - optionsDirtyAt > 1000) {
            optionsDirtyAt = -1;
            client.options.save();
        }
    }
}
