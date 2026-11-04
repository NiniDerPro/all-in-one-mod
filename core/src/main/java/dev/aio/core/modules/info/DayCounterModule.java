package dev.aio.core.modules.info;

import dev.aio.core.hud.TextHudElement;
import dev.aio.core.module.Category;

/** Aktueller Tag der Welt. */
public final class DayCounterModule extends TextHudElement {
    public DayCounterModule() {
        super("daycounter", "Tagzähler", "Wie viele Tage die Welt schon läuft", Category.INFO, false, 0.0, 0.30);
    }

    @Override protected String getText() {
        if (!platform().inWorld()) return "";
        return "Tag " + WorldTime.day(platform().dayTime());
    }

    @Override protected String previewText() { return "Tag 42"; }
}
