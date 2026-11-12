package dev.aio.core.modules.render;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.setting.BooleanSetting;

/** Removes fog: clear view under water and in lava, plus the distant fog (also nether and end). */
public final class NoFogModule extends Module {
    private final BooleanSetting water;
    private final BooleanSetting lava;
    private final BooleanSetting distance;

    public NoFogModule() {
        super("nofog", "No Fog", "Clear water and lava, no fog in the distance (also Nether and End)", Category.VISUALS, true);
        water = add(new BooleanSetting("water", "Clear water", true));
        lava = add(new BooleanSetting("lava", "Clear lava", true));
        distance = add(new BooleanSetting("distance", "No distance fog (also Nether, End)", true));
        distanceDefaultOn = add(new BooleanSetting("distanceDefaultOn", "Distance default migrated", false));
        distanceDefaultOn.hidden();
    }

    private final BooleanSetting distanceDefaultOn;

    /** The distant fog used to be on by default; set to the new default once. */
    @Override public void afterLoad() {
        super.afterLoad();
        if (!distanceDefaultOn.on()) {
            distance.set(true);
            distanceDefaultOn.set(true);
        }
    }

    public boolean clearWater() { return isActive() && water.on(); }

    public boolean clearLava() { return isActive() && lava.on(); }

    public boolean noDistanceFog() { return isActive() && distance.on(); }
}
