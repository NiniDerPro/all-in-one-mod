package dev.aio.core.modules.info;

import dev.aio.core.hud.TextHudElement;
import dev.aio.core.module.Category;

/** Aktueller Tag der Welt. */
public final class DayCounterModule extends TextHudElement {
    public DayCounterModule() {
        super("daycounter", "Day Counter", "How many days the world has been running", Category.QOL, false, 0.0, 0.30);
    }

    @Override protected String getText() {
        if (!platform().inWorld()) return "";
        return "Day " + WorldTime.day(platform().dayTime());
    }

    @Override protected String previewText() { return "Day 42"; }
}
