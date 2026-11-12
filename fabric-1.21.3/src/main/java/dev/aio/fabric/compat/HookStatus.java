package dev.aio.fabric.compat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Which AIO hooks (mixins) really apply. Filled by the {@link AioMixinPlugin} when the
 * game classes are loaded. If a hook does not apply (e.g. because another mod changes the same place),
 * only the associated module fails instead of the whole game.
 *
 * <p>Must not use Minecraft classes: it is loaded very early by the mixin system.
 */
public final class HookStatus {
    private HookStatus() {}

    /** Mixins whose application has begun. */
    static final Set<String> started = ConcurrentHashMap.newKeySet();
    /** Mixins whose application has finished. */
    static final Set<String> finished = ConcurrentHashMap.newKeySet();

    /** Which module a hook belongs to. Key: "Mixin#handler" or just "Mixin" for all hooks. */
    private static final Map<String, String> MODULE = Map.ofEntries(
            Map.entry("AtmosphericFogMixin", "nofog"), Map.entry("LavaFogMixin", "nofog"), Map.entry("WaterFogMixin", "nofog"),
            Map.entry("BlockEntityCullMixin", "entityculling"), Map.entry("EntityRendererCullMixin", "entityculling"),
            Map.entry("ClientLevelTickMixin", "entityculling"),
            Map.entry("ZoomFovMixin", "zoom"),
            Map.entry("CameraMixin#aio$freelookYaw", "freelook"), Map.entry("CameraMixin#aio$freelookPitch", "freelook"),
            Map.entry("ChatComponentMixin", "chat"),
            Map.entry("ClientLevelParticlesMixin", "particlelimit"), Map.entry("ParticleEngineMixin", "particlelimit"),
            Map.entry("EntityMixin#aio$zoomSensitivityX", "zoom"), Map.entry("EntityMixin#aio$zoomSensitivityY", "zoom"),
            Map.entry("EntityMixin#aio$freelook", "freelook"),
            Map.entry("ExperienceOrbMixin", "xpclumps"),
            Map.entry("GameRendererMixin#aio$noHurtcam", "nohurtcam"), Map.entry("GameRendererMixin#aio$noViewBobbing", "noviewbob"),
            Map.entry("SpeedFovMixin", "nospeedfov"),
            Map.entry("HudMixin", "nopumpkin"),
            Map.entry("ItemContainerContentsMixin", "shulkerpreview"),
            Map.entry("ItemStackMixin", "shulkerpreview"),
            Map.entry("LightmapMixin", "fullbright"),
            Map.entry("MinecraftFpsMixin", "dynamicfps"),
            Map.entry("MinecraftProfilerMixin", "piechart"),
            Map.entry("ChatHeadsLineMixin", "chatheads"), Map.entry("ChatHeadsDrawMixin", "chatheads"),
            Map.entry("SmallItemsMixin", "smallitems"),
            Map.entry("DynamicLightsMixin", "dynamiclights"), Map.entry("DynamicLightsEntityMixin", "dynamiclights"),
            Map.entry("ShieldIndicatorOwnerMixin", "shieldindicator"), Map.entry("ShieldIndicatorRenderMixin", "shieldindicator"),
            Map.entry("MultiPlayerGameModeMixin#aio$startBreak", "autotool"),
            Map.entry("ToolSaverMixin", "toolsaver"),
            Map.entry("LightOverlayDirtyMixin", "lightoverlay"),
            Map.entry("NametagsVanillaMixin", "nametags"),
            Map.entry("MultiPlayerGameModeMixin#aio$attack", "autoweapon"),
            Map.entry("MultiPlayerGameModeMixin", "freecam"));

    /** Module of a hook, null = basic function without its own module (keyboard/mouse). */
    static String moduleOf(String mixin, String handler) {
        String m = MODULE.get(mixin + "#" + handler);
        if (m != null) return m;
        m = MODULE.get(mixin);
        if (m != null) return m;
        if (mixin.startsWith("Freecam")) return "freecam";
        return null;
    }

    /** More simultaneous failures than this count as a measurement error: then nothing is switched off. */
    private static final int PLAUSIBLE_FAILURES = 4;

    /** Module id → description for all modules whose mixin could not be applied. */
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

    /** For the log: all mixins whose application was aborted. */
    public static List<String> missing() {
        List<String> out = new ArrayList<>();
        for (String mixin : started) if (!finished.contains(mixin)) out.add(mixin);
        return out;
    }
}
