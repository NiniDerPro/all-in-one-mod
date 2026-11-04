package dev.aio.core.modules.navigation;

import dev.aio.core.hud.TextHudElement;
import dev.aio.core.module.Category;
import dev.aio.core.platform.Vec3;
import dev.aio.core.setting.BooleanSetting;

/** Zeigt die passenden Koordinaten in der anderen Dimension (Oberwelt ÷ 8 = Nether). */
public final class PortalCalculatorModule extends TextHudElement {
    private final BooleanSetting showY;

    public PortalCalculatorModule() {
        super("portalcalc", "Portalrechner", "Oberwelt-/Nether-Koordinaten für Portale", Category.NAVIGATION, false, 0.0, 0.48);
        showY = add(new BooleanSetting("showY", "Y anzeigen", false));
    }

    @Override protected String getText() {
        if (!platform().inWorld()) return "";
        Vec3 p = platform().playerPos();
        return switch (platform().dimension()) {
            case "overworld" -> "Nether: " + coords(p.x() / 8, p.y(), p.z() / 8);
            case "the_nether" -> "Oberwelt: " + coords(p.x() * 8, p.y(), p.z() * 8);
            default -> "";
        };
    }

    private String coords(double x, double y, double z) {
        return (int) Math.floor(x) + (showY.on() ? " / " + (int) Math.floor(y) : "") + " / " + (int) Math.floor(z);
    }

    @Override protected String previewText() { return "Nether: 16 / -32"; }
}
