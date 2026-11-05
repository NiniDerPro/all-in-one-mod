package dev.aio.core.modules.render;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.setting.BooleanSetting;

/** Entfernt Nebel: klare Sicht unter Wasser und in Lava, dazu der Nebel in der Ferne (auch Nether und End). */
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

    /** Früher war der Fernnebel standardmäßig an; einmalig auf den neuen Standard setzen. */
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
