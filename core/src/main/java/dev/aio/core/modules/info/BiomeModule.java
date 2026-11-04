package dev.aio.core.modules.info;

import dev.aio.core.hud.TextHudElement;
import dev.aio.core.module.Category;
import dev.aio.core.setting.BooleanSetting;

/** Aktuelles Biom (lokalisierter Name). */
public final class BiomeModule extends TextHudElement {
    private final BooleanSetting label;

    public BiomeModule() {
        super("biome", "Biom", "Zeigt das aktuelle Biom", Category.INFO, false, 0.0, 0.24);
        label = add(new BooleanSetting("label", "Bezeichnung zeigen", true));
    }

    @Override protected String getText() {
        if (!platform().inWorld()) return "";
        return (label.on() ? "Biom: " : "") + platform().biome();
    }

    @Override protected String previewText() { return "Biom: Ebene"; }
}
