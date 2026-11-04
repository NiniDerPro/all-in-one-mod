package dev.aio.core.platform;

/** GLFW-Tastencodes. Diese sind über alle MC-Versionen stabil. */
public final class Keys {
    private Keys() {}

    public static final int NONE = -1;
    public static final int ESCAPE = 256;
    public static final int ENTER = 257;
    public static final int TAB = 258;
    public static final int BACKSPACE = 259;
    public static final int DELETE = 261;
    public static final int RIGHT = 262;
    public static final int LEFT = 263;
    public static final int LEFT_SHIFT = 340;
    public static final int LEFT_CONTROL = 341;
    public static final int LEFT_ALT = 342;
    public static final int RIGHT_SHIFT = 344;
    public static final int RIGHT_CONTROL = 345;
    public static final int C = 67;
    public static final int V = 86;

    /** Maustasten werden in Keybind-Settings als negative Codes gespeichert: -100 - button. */
    public static final int MOUSE_OFFSET = -100;

    public static int mouse(int button) { return MOUSE_OFFSET - button; }

    public static boolean isMouse(int code) { return code <= MOUSE_OFFSET; }

    public static int mouseButton(int code) { return MOUSE_OFFSET - code; }
}
