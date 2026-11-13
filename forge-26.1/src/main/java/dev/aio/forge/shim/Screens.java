package dev.aio.forge.shim;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

/** Adds widgets to a screen while it is being initialised (inside {@link ScreenEvents#AFTER_INIT}). */
public final class Screens {
    private Screens() {}

    public interface Widgets { void add(GuiEventListener widget); }

    public static Widgets getWidgets(Screen screen) {
        return widget -> {
            var add = ScreenEvents.of(screen).addWidget;
            if (add != null) add.accept(widget);
        };
    }
}
