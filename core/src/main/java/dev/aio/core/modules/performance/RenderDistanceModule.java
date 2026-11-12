package dev.aio.core.modules.performance;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.NumberSetting;

/**
 * Render distance: draw entities and block entities (chests, banners, signs, heads, …) only up to
 * a distance. Sign texts have their own, shorter range.
 */
public final class RenderDistanceModule extends Module {
    private final NumberSetting entityDistance;
    private final NumberSetting blockEntityDistance;
    private final NumberSetting signDistance;
    private final BooleanSetting keepPlayers;

    public RenderDistanceModule() {
        super("renderdistance", "Render Distance", "Only draw entities, chests and signs nearby", Category.PERFORMANCE, false);
        entityDistance = add(new NumberSetting("entities", "Entities up to", 64, 16, 256, 8, " blocks"));
        blockEntityDistance = add(new NumberSetting("blockEntities", "Block entities up to", 64, 16, 256, 8, " blocks"));
        signDistance = add(new NumberSetting("signs", "Signs up to", 24, 8, 128, 4, " blocks"));
        keepPlayers = add(new BooleanSetting("players", "Always show players", true));
    }

    public double entityDistance() { return entityDistance.get(); }

    public double blockEntityDistance() { return blockEntityDistance.get(); }

    public double signDistance() { return signDistance.get(); }

    public boolean keepPlayers() { return keepPlayers.on(); }
}
