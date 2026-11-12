package dev.aio.core.modules.performance;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.NumberSetting;
import dev.aio.core.setting.StringSetting;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity culling: mobs, items, chests, signs & co. that are hidden behind walls are
 * not drawn. The lines of sight through solid blocks are computed in a separate thread,
 * the game never waits for it. Hidden entities only get a simplified
 * tick on the client. Exceptions for big bosses and beams, leaves optionally as solid.
 */
public final class EntityCullingModule extends Module {
    private final BooleanSetting entities;
    private final BooleanSetting blockEntities;
    private final BooleanSetting tickCulling;
    private final BooleanSetting leavesSolid;
    private final BooleanSetting keepNamed;
    private final NumberSetting traceDistance;
    private final StringSetting entityWhitelist;
    private final StringSetting blockEntityWhitelist;
    private volatile Set<String> entityExceptions = Set.of();
    private volatile Set<String> blockEntityExceptions = Set.of();

    public EntityCullingModule() {
        super("entityculling", "Entity Culling", "Skip drawing mobs, chests and signs hidden behind walls", Category.PERFORMANCE, false);
        entities = add(new BooleanSetting("entities", "Entities", true));
        blockEntities = add(new BooleanSetting("blockEntities", "Block entities (chests, signs, …)", true));
        tickCulling = add(new BooleanSetting("tickCulling", "Simplified ticking for hidden entities", true));
        leavesSolid = add(new BooleanSetting("leavesSolid", "Leaves count as solid (more FPS in forests)", false));
        keepNamed = add(new BooleanSetting("keepNamed", "Always show name-tagged mobs", true));
        traceDistance = add(new NumberSetting("traceRange", "Check distance", 64, 16, 128, 8, " blocks"));
        entityWhitelist = add(new StringSetting("entityWhitelist", "Entity exceptions (IDs, comma separated)",
                "minecraft:ender_dragon,minecraft:wither,minecraft:end_crystal,minecraft:ghast,minecraft:happy_ghast", 2000));
        blockEntityWhitelist = add(new StringSetting("blockEntityWhitelist", "Block entity exceptions (IDs, comma separated)",
                "minecraft:beacon,minecraft:end_gateway,minecraft:end_portal", 2000));
        entityWhitelist.onChange(v -> entityExceptions = parse(v));
        blockEntityWhitelist.onChange(v -> blockEntityExceptions = parse(v));
        entityExceptions = parse(entityWhitelist.get());
        blockEntityExceptions = parse(blockEntityWhitelist.get());
    }

    private static Set<String> parse(String csv) {
        Set<String> out = new HashSet<>();
        for (String s : Arrays.asList(csv.split(","))) {
            String t = s.trim();
            if (!t.isEmpty()) out.add(t.contains(":") ? t : "minecraft:" + t);
        }
        return Set.copyOf(out);
    }


    public boolean cullEntities() { return isActive() && entities.on(); }

    public boolean cullBlockEntities() { return isActive() && blockEntities.on(); }

    public boolean cullTicks() { return isActive() && entities.on() && tickCulling.on(); }

    public boolean leavesSolid() { return leavesSolid.on(); }

    public boolean keepNamed() { return keepNamed.on(); }

    public double traceDistance() { return traceDistance.get(); }

    public boolean isEntityException(String id) { return entityExceptions.contains(id); }

    public boolean isBlockEntityException(String id) { return blockEntityExceptions.contains(id); }
}
