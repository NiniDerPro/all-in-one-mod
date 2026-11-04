package dev.aio.core.platform;

import java.util.HashSet;
import java.util.Set;

/**
 * Regeln für AIO-Hotkeys: Ein Hotkey zählt nur allein. Wird dazu noch eine "Kombi-Taste" gehalten
 * (F1–F25, Strg, Alt, Windows/Cmd, Tab), passiert nichts – F3+F4 löst also F4 nicht aus.
 * Ist der Hotkey selbst Strg/Alt/Windows, wird er erst beim Loslassen ausgelöst und nur, wenn
 * dazwischen keine andere Taste kam und das Fenster noch den Fokus hat (Alt+Tab löst Alt nicht aus).
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

    /** Ob außer {@code key} gerade eine Kombi-Taste gehalten wird. */
    public static boolean otherComboHeld(Platform p, String key) {
        for (String k : COMBO) if (!k.equals(key) && p.isKeyDown(k)) return true;
        return false;
    }

    /**
     * Merkt sich einen gedrückten Modifier-Hotkey bis zum Loslassen. Jede andere Taste dazwischen
     * oder ein Fokusverlust bricht ab.
     */
    public static final class Tap {
        private String pending;

        /** Vor der Auswertung jedes Tastendrucks aufrufen: andere Taste = Abbruch. */
        public void pressed(String key) {
            if (pending != null && !pending.equals(key)) pending = null;
        }

        public void arm(String key) { pending = key; }

        /** true = der gemerkte Modifier wurde allein getippt und soll jetzt auslösen. */
        public boolean released(String key, boolean focused) {
            if (pending == null || !pending.equals(key)) return false;
            pending = null;
            return focused;
        }

        public void cancel() { pending = null; }
    }
}
