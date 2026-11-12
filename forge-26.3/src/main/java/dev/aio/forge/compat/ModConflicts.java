package dev.aio.forge.compat;

import net.fabricmc.loader.api.FabricLoader;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Other mods with the same function as an AIO module. If one of them is installed it takes
 * priority: the AIO module stays off (can be switched off via "Prefer other mods" in Client).
 */
public final class ModConflicts {
    private ModConflicts() {}

    /** AIO module → mod ids with the same function. */
    private static final Map<String, List<String>> TABLE = Map.ofEntries(
            Map.entry("waypoints", List.of("xaerominimap", "xaerominimapfair", "journeymap", "voxelmap")),
            Map.entry("deathpoint", List.of("xaerominimap", "xaerominimapfair", "journeymap", "voxelmap")),
            Map.entry("freecam", List.of("freecam")),
            Map.entry("chatheads", List.of("chat_heads")),
            Map.entry("itemlist", List.of("jei", "roughlyenoughitems", "emi")),
            Map.entry("foodinfo", List.of("appleskin")),
            Map.entry("entityculling", List.of("entityculling")),
            Map.entry("xpclumps", List.of("clumps")),
            Map.entry("dynamicfps", List.of("dynamic_fps")),
            Map.entry("keybinds", List.of("controlling")),
            Map.entry("shulkerpreview", List.of("shulkerboxtooltip")),
            Map.entry("zoom", List.of("zoomify", "wi_zoom", "ok_zoomer", "logical_zoom")),
            Map.entry("freelook", List.of("freelook", "perspectivemod", "perspective")),
            Map.entry("invtweaks", List.of("mousetweaks")),
            Map.entry("lookat", List.of("jade", "wthit")),
            Map.entry("chat", List.of("chatpatches")),
            Map.entry("fullbright", List.of("gammautils", "fullbrightnesstoggle")),
            Map.entry("nofog", List.of("sodium-extra")),
            Map.entry("shieldindicator", List.of("shieldstatuses", "shield-statuses", "shieldstatus", "sentinelshield")),
            Map.entry("lightoverlay", List.of("lightoverlay", "light-overlay")),
            Map.entry("toolsaver", List.of("toolsaver", "durabilitywarning", "durability-warning", "damagewarning")),
            Map.entry("healthtags", List.of("healthindicator", "health-indicator", "healthindicators", "neat")),
            Map.entry("dynamiclights", List.of("lambdynlights", "sodiumdynamiclights", "dynamiclights", "ryoamiclights")),
            Map.entry("nametags", List.of("meteor-client")),
            Map.entry("togglesprint", List.of("togglesprint")),
            Map.entry("elytraswap", List.of("elytraswap", "elytra-swapper")));

    /** Module id → name of the installed mod that takes priority. */
    public static Map<String, String> installed() {
        Map<String, String> out = new LinkedHashMap<>();
        FabricLoader loader = FabricLoader.getInstance();
        for (Map.Entry<String, List<String>> e : TABLE.entrySet()) {
            for (String mod : e.getValue()) {
                var c = loader.getModContainer(mod);
                if (c.isPresent()) {
                    out.put(e.getKey(), c.get().getMetadata().getName());
                    break;
                }
            }
        }
        return out;
    }
}
