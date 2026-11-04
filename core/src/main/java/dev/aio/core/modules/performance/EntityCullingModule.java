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
 * Entity-Culling: Mobs, Items, Truhen, Schilder & Co., die hinter Wänden verborgen sind, werden
 * nicht gezeichnet. Die Sichtlinien durch feste Blöcke werden in einem eigenen Thread berechnet,
 * das Spiel wartet nie darauf. Verdeckte Entities bekommen auf dem Client nur einen vereinfachten
 * Tick. Ausnahmen für große Bosse und Strahlen, Laub optional als fest.
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
        super("entityculling", "Entity-Culling", "Verdeckte Mobs, Truhen und Schilder nicht zeichnen", Category.PERFORMANCE, false);
        entities = add(new BooleanSetting("entities", "Entities", true));
        blockEntities = add(new BooleanSetting("blockEntities", "Block-Entities (Truhen, Schilder, …)", true));
        tickCulling = add(new BooleanSetting("tickCulling", "Verdeckte Entities vereinfacht ticken", true));
        leavesSolid = add(new BooleanSetting("leavesSolid", "Laub zählt als fest (mehr FPS in Wäldern)", false));
        keepNamed = add(new BooleanSetting("keepNamed", "Mobs mit Namensschild immer zeigen", true));
        traceDistance = add(new NumberSetting("traceRange", "Prüfweite", 64, 16, 128, 8, " Blöcke"));
        entityWhitelist = add(new StringSetting("entityWhitelist", "Ausnahmen Entities (IDs mit Komma)",
                "minecraft:ender_dragon,minecraft:wither,minecraft:end_crystal,minecraft:ghast,minecraft:happy_ghast", 2000));
        blockEntityWhitelist = add(new StringSetting("blockEntityWhitelist", "Ausnahmen Block-Entities (IDs mit Komma)",
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

    @Override public boolean enabledByDefault() { return true; }

    public boolean cullEntities() { return isActive() && entities.on(); }

    public boolean cullBlockEntities() { return isActive() && blockEntities.on(); }

    public boolean cullTicks() { return isActive() && entities.on() && tickCulling.on(); }

    public boolean leavesSolid() { return leavesSolid.on(); }

    public boolean keepNamed() { return keepNamed.on(); }

    public double traceDistance() { return traceDistance.get(); }

    public boolean isEntityException(String id) { return entityExceptions.contains(id); }

    public boolean isBlockEntityException(String id) { return blockEntityExceptions.contains(id); }
}
