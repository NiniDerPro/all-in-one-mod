package dev.aio.fabric.compat;

import net.fabricmc.loader.api.FabricLoader;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Andere Mods mit derselben Funktion wie ein AIO-Modul. Ist eine davon installiert, hat sie
 * Vorrang: das AIO-Modul bleibt aus (abschaltbar über "Prefer other mods" in Client).
 */
public final class ModConflicts {
    private ModConflicts() {}

    /** AIO-Modul → Mod-IDs mit derselben Funktion. */
    private static final Map<String, List<String>> TABLE = Map.ofEntries(
            Map.entry("waypoints", List.of("xaerominimap", "xaerominimapfair", "journeymap", "voxelmap")),
            Map.entry("deathpoint", List.of("xaerominimap", "xaerominimapfair", "journeymap", "voxelmap")),
            Map.entry("freecam", List.of("freecam")),
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
            Map.entry("togglesprint", List.of("togglesprint")),
            Map.entry("elytraswap", List.of("elytraswap", "elytra-swapper")));

    /** Modul-ID → Name der installierten Mod, die Vorrang hat. */
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
