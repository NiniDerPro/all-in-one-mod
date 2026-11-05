package dev.aio.fabric.compat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
            Map.entry("DynamicLightsMixin", "dynamiclights"), Map.entry("DynamicLightsEntityMixin", "dynamiclights"),
            Map.entry("ShieldIndicatorOwnerMixin", "shieldindicator"), Map.entry("ShieldIndicatorRenderMixin", "shieldindicator"),
            Map.entry("MultiPlayerGameModeMixin#aio$startBreak", "autotool"),
            Map.entry("ToolSaverMixin", "toolsaver"),
            Map.entry("LightOverlayDirtyMixin", "lightoverlay"),
            Map.entry("NametagsVanillaMixin", "nametags"),
            Map.entry("MultiPlayerGameModeMixin#aio$attack", "autoweapon"),
            Map.entry("MultiPlayerGameModeMixin", "freecam"));

    /** Modul eines Eingriffs, null = Grundfunktion ohne eigenes Modul (Tastatur/Maus). */
    static String moduleOf(String mixin, String handler) {
        String m = MODULE.get(mixin + "#" + handler);
        if (m != null) return m;
        m = MODULE.get(mixin);
        if (m != null) return m;
        if (mixin.startsWith("Freecam")) return "freecam";
        return null;
    }

    /** Mehr gleichzeitige Ausfälle als das gelten als Messfehler: dann wird nichts abgeschaltet. */
    private static final int PLAUSIBLE_FAILURES = 4;

    /** Modul-ID → Beschreibung für alle Module, deren Mixin nicht eingebaut werden konnte. */
    public static Map<String, String> broken() {
        Map<String, String> out = new LinkedHashMap<>();
        for (String mixin : started) {
            if (finished.contains(mixin)) continue;
            for (String key : MODULE.keySet()) {
                if (key.equals(mixin) || key.startsWith(mixin + "#")) out.putIfAbsent(MODULE.get(key), "conflict in " + mixin);
            }
            if (mixin.startsWith("Freecam")) out.putIfAbsent("freecam", "conflict in " + mixin);
        }
        return out.size() > PLAUSIBLE_FAILURES ? Map.of() : out;
    }

    /** Für das Log: alle Mixins, deren Einbau abgebrochen ist. */
    public static List<String> missing() {
        List<String> out = new ArrayList<>();
        for (String mixin : started) if (!finished.contains(mixin)) out.add(mixin);
        return out;
    }
}
