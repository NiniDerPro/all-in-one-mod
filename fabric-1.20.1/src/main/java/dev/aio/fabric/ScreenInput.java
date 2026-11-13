package dev.aio.fabric;

import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Mouse dragging and typed characters on screens. The Fabric API of this Minecraft version has no events for
 * them, so two small mixins ({@code ScreenDragMixin}, {@code ScreenCharMixin}) call in here.
 */
public final class ScreenInput {
    private ScreenInput() {}

    /** Return false to swallow the drag. */
    public interface Drag { boolean allow(double mouseX, double mouseY, int button, double dx, double dy); }

    /** Return false to swallow the character. */
    public interface Char { boolean allow(String character); }

    private static final Map<Screen, List<Drag>> DRAG = new WeakHashMap<>();
    private static final Map<Screen, List<Char>> CHARS = new WeakHashMap<>();

    static void onDrag(Screen screen, Drag listener) { DRAG.computeIfAbsent(screen, s -> new ArrayList<>()).add(listener); }

    static void onChar(Screen screen, Char listener) { CHARS.computeIfAbsent(screen, s -> new ArrayList<>()).add(listener); }

    /** A screen is initialised again or closed: its listeners are registered anew. */
    static void clear(Screen screen) {
        DRAG.remove(screen);
        CHARS.remove(screen);
    }

    public static boolean allowDrag(Screen screen, double mouseX, double mouseY, int button, double dx, double dy) {
        List<Drag> list = DRAG.get(screen);
        if (list == null) return true;
        for (Drag d : list) if (!d.allow(mouseX, mouseY, button, dx, dy)) return false;
        return true;
    }

    public static boolean allowChar(Screen screen, int codePoint) {
        List<Char> list = CHARS.get(screen);
        if (list == null) return true;
        String s = Character.toString(codePoint);
        for (Char c : list) if (!c.allow(s)) return false;
        return true;
    }
}
