package dev.aio.fabric;

import java.util.HashSet;
import java.util.Set;

/**
 * Welche Tasten gerade gehalten werden, gesammelt aus den Eingabe-Events.
 * Unabhängig vom Eingabe-Backend (26.x nutzt SDL statt GLFW).
 */
public final class InputState {
    private static final Set<String> DOWN = new HashSet<>();

    private InputState() {}

    public static void update(String key, int action) {
        if (key == null || key.isEmpty()) return;
        if (action == 0) DOWN.remove(key);
        else DOWN.add(key);
    }

    public static boolean isDown(String key) { return DOWN.contains(key); }

    /** Z. B. wenn das Fenster den Fokus verliert. */
    public static void clear() { DOWN.clear(); }
}
