package dev.aio.fabric.compat;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Welche AIO-Eingriffe (Mixins) wirklich greifen. Wird vom {@link AioMixinPlugin} beim Laden der
 * Spielklassen gefüllt. Greift ein Eingriff nicht (z. B. weil eine andere Mod dieselbe Stelle
 * verändert), fällt nur das zugehörige Modul aus statt des ganzen Spiels.
 *
 * <p>Darf keine Minecraft-Klassen benutzen: wird sehr früh vom Mixin-System geladen.
 */
public final class HookStatus {
    private HookStatus() {}

    /** Mixins, deren Einbau begonnen hat. */
    static final Set<String> started = ConcurrentHashMap.newKeySet();
    /** Mixins, deren Einbau fertig wurde. */
    static final Set<String> finished = ConcurrentHashMap.newKeySet();
    /** "Mixin#handler" → greift der Eingriff? */
    static final Map<String, Boolean> handlers = new ConcurrentHashMap<>();

    /** Eingriffe, die fehlen dürfen (nur Komfort, Modul funktioniert auch ohne). */
    private static final Set<String> OPTIONAL = Set.of(
            "FreecamLocalPlayerMixin#aio$freecamControlled", "CameraMixin#aio$zoomHand",
            "ChatComponentMixin#aio$timestamp", "ChatComponentMixin#aio$historySize",
            "EntityMixin#aio$zoomSensitivityX", "EntityMixin#aio$zoomSensitivityY",
            "ItemContainerContentsMixin#aio$hideContentLines", "ClientLevelTickMixin#aio$tickCull");

    /** Zu welchem Modul ein Eingriff gehört. Schlüssel: "Mixin#handler" oder nur "Mixin" für alle Eingriffe. */
    private static final Map<String, String> MODULE = Map.ofEntries(
            Map.entry("AtmosphericFogMixin", "nofog"), Map.entry("LavaFogMixin", "nofog"), Map.entry("WaterFogMixin", "nofog"),
            Map.entry("BlockEntityCullMixin", "entityculling"), Map.entry("EntityRendererCullMixin", "entityculling"),
            Map.entry("ClientLevelTickMixin", "entityculling"),
            Map.entry("CameraMixin#aio$zoom", "zoom"), Map.entry("CameraMixin#aio$zoomHand", "zoom"),
            Map.entry("CameraMixin#aio$freelookYaw", "freelook"), Map.entry("CameraMixin#aio$freelookPitch", "freelook"),
            Map.entry("ChatComponentMixin", "chat"),
            Map.entry("ClientLevelParticlesMixin", "particlelimit"), Map.entry("ParticleEngineMixin", "particlelimit"),
            Map.entry("EntityMixin#aio$zoomSensitivityX", "zoom"), Map.entry("EntityMixin#aio$zoomSensitivityY", "zoom"),
            Map.entry("EntityMixin#aio$freelook", "freelook"),
            Map.entry("ExperienceOrbMixin", "xpclumps"),
            Map.entry("GameRendererMixin", "nohurtcam"),
            Map.entry("HudMixin", "nopumpkin"),
            Map.entry("ItemContainerContentsMixin", "shulkerpreview"),
            Map.entry("ItemStackMixin", "shulkerpreview"),
            Map.entry("LightmapMixin", "fullbright"),
            Map.entry("MinecraftFpsMixin", "dynamicfps"),
            Map.entry("MinecraftProfilerMixin", "piechart"),
            Map.entry("SmallItemsMixin", "smallitems"),
            Map.entry("ToolSaverMixin", "toolsaver"),
            Map.entry("DynamicLightsMixin", "dynamiclights"), Map.entry("DynamicLightsEntityMixin", "dynamiclights"),
            Map.entry("ShieldIndicatorOwnerMixin", "shieldindicator"), Map.entry("ShieldIndicatorRenderMixin", "shieldindicator"),
            Map.entry("MultiPlayerGameModeMixin#aio$startBreak", "autotool"),
            Map.entry("MultiPlayerGameModeMixin#aio$attack", "autoweapon"),
            Map.entry("MultiPlayerGameModeMixin", "freecam"));

    static boolean optional(String key) { return OPTIONAL.contains(key); }

    /** Modul eines Eingriffs, null = Grundfunktion ohne eigenes Modul (Tastatur/Maus). */
    static String moduleOf(String mixin, String handler) {
        String m = MODULE.get(mixin + "#" + handler);
        if (m != null) return m;
        m = MODULE.get(mixin);
        if (m != null) return m;
        if (mixin.startsWith("Freecam")) return "freecam";
        return null;
    }

    /** Modul-ID → Beschreibung für alle Module, deren Eingriff nicht greift. */
    public static Map<String, String> broken() {
        Map<String, String> out = new java.util.LinkedHashMap<>();
        for (String mixin : started) {
            if (finished.contains(mixin)) continue;
            // Einbau ganz gescheitert: alle Module dieses Mixins
            for (String key : MODULE.keySet()) {
                if (key.equals(mixin) || key.startsWith(mixin + "#")) out.putIfAbsent(MODULE.get(key), "conflict in " + mixin);
            }
            if (mixin.startsWith("Freecam")) out.putIfAbsent("freecam", "conflict in " + mixin);
        }
        for (Map.Entry<String, Boolean> e : handlers.entrySet()) {
            if (e.getValue() || optional(e.getKey())) continue;
            String[] parts = e.getKey().split("#", 2);
            String module = moduleOf(parts[0], parts[1]);
            if (module != null) out.putIfAbsent(module, "conflict in " + parts[0]);
        }
        return out;
    }

    /** Für das Log: alle fehlenden Eingriffe, auch die ohne eigenes Modul. */
    public static java.util.List<String> missing() {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String mixin : started) if (!finished.contains(mixin)) out.add(mixin + " (not applied)");
        for (Map.Entry<String, Boolean> e : handlers.entrySet()) if (!e.getValue()) out.add(e.getKey());
        return out;
    }
}
