package dev.aio.neoforge.shim;

import net.minecraft.client.gui.screens.Screen;

/** Mouse hooks of a screen (see {@link ScreenEvents}). A listener returning false swallows the input. */
public final class ScreenMouseEvents {
    private ScreenMouseEvents() {}

    public static ScreenEvents.Hook<ScreenEvents.AllowMouse> allowMouseClick(Screen screen) { return ScreenEvents.of(screen).click; }
    public static ScreenEvents.Hook<ScreenEvents.AllowMouse> allowMouseRelease(Screen screen) { return ScreenEvents.of(screen).release; }
    public static ScreenEvents.Hook<ScreenEvents.AllowDrag> allowMouseDrag(Screen screen) { return ScreenEvents.of(screen).drag; }
    public static ScreenEvents.Hook<ScreenEvents.AfterRelease> afterMouseRelease(Screen screen) { return ScreenEvents.of(screen).afterRelease; }
    public static ScreenEvents.Hook<ScreenEvents.AllowScroll> allowMouseScroll(Screen screen) { return ScreenEvents.of(screen).scroll; }
}
