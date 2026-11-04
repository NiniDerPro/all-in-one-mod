package dev.aio.core.modules.performance;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.NumberSetting;

/**
 * Render-Reichweite: Entities und Block-Entities (Truhen, Banner, Schilder, Köpfe, …) nur bis zu
 * einer Entfernung zeichnen. Schildertexte haben eine eigene, kürzere Reichweite.
 */
public final class RenderDistanceModule extends Module {
    private final NumberSetting entityDistance;
    private final NumberSetting blockEntityDistance;
    private final NumberSetting signDistance;
    private final BooleanSetting keepPlayers;

    public RenderDistanceModule() {
        super("renderdistance", "Render-Reichweite", "Entities, Truhen und Schilder nur in der Nähe zeichnen", Category.PERFORMANCE, false);
        entityDistance = add(new NumberSetting("entities", "Entities bis", 64, 16, 256, 8, " Blöcke"));
        blockEntityDistance = add(new NumberSetting("blockEntities", "Block-Entities bis", 64, 16, 256, 8, " Blöcke"));
        signDistance = add(new NumberSetting("signs", "Schilder bis", 24, 8, 128, 4, " Blöcke"));
        keepPlayers = add(new BooleanSetting("players", "Spieler immer zeigen", true));
    }

    public double entityDistance() { return entityDistance.get(); }

    public double blockEntityDistance() { return blockEntityDistance.get(); }

    public double signDistance() { return signDistance.get(); }

    public boolean keepPlayers() { return keepPlayers.on(); }
}
