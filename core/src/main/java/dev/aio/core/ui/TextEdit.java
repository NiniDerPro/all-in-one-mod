package dev.aio.core.ui;

import dev.aio.core.platform.Keys;
import dev.aio.core.platform.Platform;
import dev.aio.core.platform.Renderer;

/**
 * Shared key logic for simple text fields: backspace/delete, Ctrl+A (select all),
 * Ctrl+C/X/V. When everything is selected, typing or pasting replaces the text, backspace deletes it.
 */
public final class TextEdit {
    private TextEdit() {}

    /** New text and whether everything is still selected afterwards; {@code handled} = key processed. */
    public record Result(String text, boolean allSelected, boolean handled) {}

    public static boolean ctrl(Platform p) {
        return p.isKeyDown(Keys.LEFT_CONTROL) || p.isKeyDown(Keys.RIGHT_CONTROL);
    }

    public static Result key(Platform p, String text, boolean all, String key) {
        boolean ctrl = ctrl(p);
        if (ctrl && "key.keyboard.a".equals(key)) return new Result(text, !text.isEmpty(), true);
        if (ctrl && Keys.C.equals(key)) {
            p.setClipboard(text);
            return new Result(text, all, true);
        }
        if (ctrl && "key.keyboard.x".equals(key)) {
            p.setClipboard(text);
            return new Result("", false, true);
        }
        if (ctrl && Keys.V.equals(key)) {
            String paste = p.clipboard().replaceAll("[\\r\\n\\t]", "");
            return new Result(all ? paste : text + paste, false, true);
        }
        if (Keys.BACKSPACE.equals(key) || Keys.DELETE.equals(key)) {
            if (all) return new Result("", false, true);
            if (Keys.BACKSPACE.equals(key) && !text.isEmpty()) {
                return new Result(text.substring(0, text.offsetByCodePoints(text.length(), -1)), false, true);
            }
            return new Result(text, false, true);
        }
        // Arrows/Home/End remove the selection
        if ("key.keyboard.left".equals(key) || "key.keyboard.right".equals(key)
                || "key.keyboard.home".equals(key) || "key.keyboard.end".equals(key)) {
            return new Result(text, false, all);
        }
        return new Result(text, all, false);
    }

    /** Typed text: replaces everything when selected. */
    public static String type(String text, boolean all, String typed) {
        return all ? typed : text + typed;
    }

    /** Selection background behind the visible text. */
    public static void drawSelection(Renderer r, float x, float y, String visible) {
        if (visible.isEmpty()) return;
        r.fill(x - 1, y - 1, x + r.textWidth(visible) + 1, y + r.fontHeight(), 0xFF3355CC);
    }
}
