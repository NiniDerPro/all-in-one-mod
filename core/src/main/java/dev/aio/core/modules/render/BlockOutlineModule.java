package dev.aio.core.modules.render;

import dev.aio.core.hud.ColorMode;
import dev.aio.core.hud.ColoredText;
import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.ColorSetting;
import dev.aio.core.setting.EnumSetting;
import dev.aio.core.setting.NumberSetting;

/** Eigene Block-Umrandung: Farbe (auch Regenbogen), Dicke, Füllung. */
public final class BlockOutlineModule extends Module {
    private final EnumSetting<ColorMode> colorMode;
    private final ColorSetting lineColor;
    private final ColorSetting secondColor;
    private final NumberSetting thickness;
    private final BooleanSetting fill;
    private final ColorSetting fillColor;

    public BlockOutlineModule() {
        super("blockoutline", "Block Outline", "Customize the outline of the block you look at", Category.VISUALS, false);
        colorMode = add(new EnumSetting<>("colorMode", "Color mode", ColorMode.STATIC));
        lineColor = add(new ColorSetting("color", "Line color", 0xFFFFFFFF));
        lineColor.visibleWhen(() -> colorMode.get() != ColorMode.CHROMA);
        secondColor = add(new ColorSetting("secondColor", "Second color", 0xFF4FC3FF));
        secondColor.visibleWhen(() -> colorMode.get() == ColorMode.GRADIENT);
        thickness = add(new NumberSetting("thickness", "Thickness", 2, 1, 8, 0.5, " px"));
        fill = add(new BooleanSetting("fill", "Fill", false));
        fillColor = add(new ColorSetting("fillColor", "Fill color", 0x30FFFFFF));
        fillColor.visibleWhen(fill::on);
    }

    /** Aktuelle Linienfarbe (ARGB), animiert bei Regenbogen/Verlauf. */
    public int lineColor() {
        return ColoredText.color(colorMode.get(), lineColor.argb(), secondColor.argb(), 0.3f, 0);
    }

    /** Liniendicke in Blöcken (1 px ≈ 1/400 Block, passt optisch zur Vanilla-Linie). */
    public float thicknessBlocks() { return thickness.floatValue() / 400f; }

    public boolean fill() { return fill.on(); }

    public int fillColor() { return fillColor.argb(); }
}
