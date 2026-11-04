package dev.aio.core.modules.render;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.setting.BooleanSetting;

/** Entfernt Nebel: klare Sicht unter Wasser und in Lava, optional auch den Sichtweitennebel. */
public final class NoFogModule extends Module {
    private final BooleanSetting water;
    private final BooleanSetting lava;
    private final BooleanSetting distance;

    public NoFogModule() {
        super("nofog", "No Fog", "Clear water and lava, optionally no distance fog", Category.VISUALS, true);
        water = add(new BooleanSetting("water", "Clear water", true));
        lava = add(new BooleanSetting("lava", "Clear lava", true));
        distance = add(new BooleanSetting("distance", "No distance fog", false));
    }

    public boolean clearWater() { return isActive() && water.on(); }

    public boolean clearLava() { return isActive() && lava.on(); }

    public boolean noDistanceFog() { return isActive() && distance.on(); }
}
