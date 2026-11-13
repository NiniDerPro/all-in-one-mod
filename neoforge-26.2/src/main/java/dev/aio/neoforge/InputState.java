package dev.aio.neoforge;

import java.util.HashSet;
import java.util.Set;

/**
 * Which keys are currently held, collected from the input events.
 * Independent of the input backend (26.x uses SDL instead of GLFW).
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

    /** E.g. when the window loses focus. */
    public static void clear() { DOWN.clear(); }
}
