package dev.aio.neoforge.shim;

import net.minecraft.client.gui.screens.Screen;

/** Keyboard hooks of a screen (see {@link ScreenEvents}). A listener returning false swallows the input. */
public final class ScreenKeyboardEvents {
    private ScreenKeyboardEvents() {}

    public static ScreenEvents.Hook<ScreenEvents.AllowKey> allowKeyPress(Screen screen) { return ScreenEvents.of(screen).key; }
    public static ScreenEvents.Hook<ScreenEvents.AllowChar> allowCharType(Screen screen) { return ScreenEvents.of(screen).chr; }
}
