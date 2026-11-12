package dev.aio.core.platform;

import java.util.HashSet;
import java.util.Set;

/**
 * Rules for AIO hotkeys: a hotkey only counts alone. If a "combo key" is also held
 * (F1–F25, Ctrl, Alt, Windows/Cmd, Tab), nothing happens – so F3+F4 does not trigger F4.
 * If the hotkey itself is Ctrl/Alt/Windows, it only fires on release and only if
 * no other key came in between and the window still has focus (Alt+Tab does not trigger Alt).
 */
public final class Hotkeys {
    private static final Set<String> COMBO = new HashSet<>();
    private static final Set<String> MODIFIER = Set.of(
            "key.keyboard.left.control", "key.keyboard.right.control",
            "key.keyboard.left.alt", "key.keyboard.right.alt",
            "key.keyboard.left.win", "key.keyboard.right.win");

    static {
        for (int i = 1; i <= 25; i++) COMBO.add("key.keyboard.f" + i);
        COMBO.addAll(MODIFIER);
        COMBO.add("key.keyboard.tab");
        COMBO.add("key.keyboard.menu");
    }

    private Hotkeys() {}

    public static boolean isCombo(String key) { return COMBO.contains(key); }

    public static boolean isModifier(String key) { return MODIFIER.contains(key); }

    /** Whether a combo key other than {@code key} is currently held. */
    public static boolean otherComboHeld(Platform p, String key) {
        for (String k : COMBO) if (!k.equals(key) && p.isKeyDown(k)) return true;
        return false;
    }

    /**
     * Remembers a pressed modifier hotkey until release. Any other key in between
     * or a loss of focus aborts.
     */
    public static final class Tap {
        private String pending;

        /** Call before evaluating every key press: other key = abort. */
        public void pressed(String key) {
            if (pending != null && !pending.equals(key)) pending = null;
        }

        public void arm(String key) { pending = key; }

        /** true = the remembered modifier was tapped alone and should fire now. */
        public boolean released(String key, boolean focused) {
            if (pending == null || !pending.equals(key)) return false;
            pending = null;
            return focused;
        }

        public void cancel() { pending = null; }
    }
}
