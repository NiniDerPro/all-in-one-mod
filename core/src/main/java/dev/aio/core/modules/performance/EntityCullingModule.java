package dev.aio.core.modules.performance;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.NumberSetting;

/**
 * Entity-Culling: Mobs, Items, Truhen, Schilder & Co., die hinter Wänden verborgen sind, werden
 * nicht gezeichnet (Sichtlinie durch feste Blöcke wird alle paar Ticks geprüft). Unsichtbare
 * Entities bekommen auf dem Client nur einen vereinfachten Tick. Spart viel in Höhlen und Basen.
 */
public final class EntityCullingModule extends Module {
    private final BooleanSetting entities;
    private final BooleanSetting blockEntities;
    private final BooleanSetting tickCulling;
    private final NumberSetting traceDistance;
    private final NumberSetting refreshTicks;

    public EntityCullingModule() {
        super("entityculling", "Entity-Culling", "Verdeckte Mobs, Truhen und Schilder nicht zeichnen", Category.PERFORMANCE, false);
        entities = add(new BooleanSetting("entities", "Entities", true));
        blockEntities = add(new BooleanSetting("blockEntities", "Block-Entities (Truhen, Schilder, …)", true));
        tickCulling = add(new BooleanSetting("tickCulling", "Verdeckte Entities vereinfacht ticken", true));
        traceDistance = add(new NumberSetting("traceDistance", "Prüfweite", 96, 16, 256, 8, " Blöcke"));
        refreshTicks = add(new NumberSetting("refresh", "Neu prüfen alle", 3, 1, 10, 1, " Ticks"));
    }

    @Override public boolean enabledByDefault() { return true; }

    public boolean cullEntities() { return isActive() && entities.on(); }

    public boolean cullBlockEntities() { return isActive() && blockEntities.on(); }

    public boolean cullTicks() { return isActive() && entities.on() && tickCulling.on(); }

    public double traceDistance() { return traceDistance.get(); }

    public int refreshTicks() { return refreshTicks.intValue(); }
}
