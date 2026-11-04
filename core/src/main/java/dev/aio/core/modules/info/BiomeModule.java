package dev.aio.core.modules.info;

import dev.aio.core.hud.TextHudElement;
import dev.aio.core.module.Category;
import dev.aio.core.setting.BooleanSetting;

/** Aktuelles Biom (lokalisierter Name). */
public final class BiomeModule extends TextHudElement {
    private final BooleanSetting label;

    public BiomeModule() {
        super("biome", "Biome", "Shows the current biome", Category.INFO, false, 0.0, 0.24);
        label = add(new BooleanSetting("label", "Show label", true));
    }

    @Override protected String getText() {
        if (!platform().inWorld()) return "";
        return (label.on() ? "Biome: " : "") + platform().biome();
    }

    @Override protected String previewText() { return "Biome: Plains"; }
}
