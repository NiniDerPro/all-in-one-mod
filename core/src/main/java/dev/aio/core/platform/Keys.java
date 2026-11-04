package dev.aio.core.platform;

/**
 * Tasten werden über ihre Minecraft-Namen identifiziert (z. B. {@code key.keyboard.right.shift},
 * {@code key.mouse.left}). Diese Namen sind über MC-Versionen und Eingabe-Backends (GLFW, SDL)
 * hinweg stabil, numerische Codes nicht.
 */
public final class Keys {
    private Keys() {}

    public static final String NONE = "";
    public static final String ESCAPE = "key.keyboard.escape";
    public static final String ENTER = "key.keyboard.enter";
    public static final String BACKSPACE = "key.keyboard.backspace";
    public static final String DELETE = "key.keyboard.delete";
    public static final String LEFT_SHIFT = "key.keyboard.left.shift";
    public static final String RIGHT_SHIFT = "key.keyboard.right.shift";
    public static final String LEFT_CONTROL = "key.keyboard.left.control";
    public static final String RIGHT_CONTROL = "key.keyboard.right.control";
    public static final String LEFT_ALT = "key.keyboard.left.alt";
    public static final String C = "key.keyboard.c";
    public static final String R = "key.keyboard.r";
    public static final String V = "key.keyboard.v";

    private static final String MOUSE_PREFIX = "key.mouse.";

    /** Name einer Maustaste (0 = links, 1 = rechts, 2 = Mitte, 3+ = Seitentasten). */
    public static String mouse(int button) {
        return switch (button) {
            case 0 -> "key.mouse.left";
            case 1 -> "key.mouse.right";
            case 2 -> "key.mouse.middle";
            default -> MOUSE_PREFIX + (button + 1);
        };
    }

    public static boolean isMouse(String key) { return key != null && key.startsWith(MOUSE_PREFIX); }

    /** Maustaste zu einem Namen oder -1. */
    public static int mouseButton(String key) {
        if (!isMouse(key)) return -1;
        String rest = key.substring(MOUSE_PREFIX.length());
        return switch (rest) {
            case "left" -> 0;
            case "right" -> 1;
            case "middle" -> 2;
            default -> {
                try {
                    yield Integer.parseInt(rest) - 1;
                } catch (NumberFormatException e) {
                    yield -1;
                }
            }
        };
    }

    public static boolean isNone(String key) { return key == null || key.isEmpty(); }
}
