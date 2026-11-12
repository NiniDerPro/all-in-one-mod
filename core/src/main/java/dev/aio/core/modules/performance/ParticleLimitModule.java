package dev.aio.core.modules.performance;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.setting.ActionSetting;
import dev.aio.core.setting.NumberSetting;
import dev.aio.core.setting.StringSetting;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

/**
 * Particles: every particle type can be switched off individually (with search and the most important on top), plus a
 * general limit for what percentage of the remaining particles appear.
 */
public final class ParticleLimitModule extends Module {
    /** Own ids for particles that Minecraft creates without its own particle type. */
    public static final String BLOCK_BREAK = "aio:block_break";
    public static final String BLOCK_CRACK = "aio:block_crack";

    /** Frequently switched-off particles with display names (at the top of the list). */
    public static final Map<String, String> IMPORTANT = new LinkedHashMap<>();

    static {
        String[][] list = {
                {BLOCK_BREAK, "Block broken (debris)"}, {BLOCK_CRACK, "Mining a block (debris while hitting)"},
                {"minecraft:block", "Block dust (landing, sprinting)"}, {"minecraft:explosion", "Explosion"},
                {"minecraft:explosion_emitter", "Large explosion"}, {"minecraft:poof", "Smoke puff (mob death)"},
                {"minecraft:crit", "Critical hit"}, {"minecraft:enchanted_hit", "Enchanted hit"},
                {"minecraft:damage_indicator", "Damage indicator (hearts)"}, {"minecraft:sweep_attack", "Sweep attack"},
                {"minecraft:totem_of_undying", "Totem of Undying"}, {"minecraft:entity_effect", "Potion Effects"},
                {"minecraft:firework", "Fireworks"}, {"minecraft:campfire_cosy_smoke", "Campfire smoke"},
                {"minecraft:campfire_signal_smoke", "Signal fire smoke"}, {"minecraft:smoke", "Smoke"},
                {"minecraft:large_smoke", "Large smoke"}, {"minecraft:rain", "Rain drops"},
                {"minecraft:portal", "Portal"}, {"minecraft:heart", "Hearts (breeding)"},
                {"minecraft:angry_villager", "Angry villager"}, {"minecraft:happy_villager", "Happy villager"},
                {"minecraft:dust", "Redstone dust"}, {"minecraft:falling_dust", "Falling dust"},
                {"minecraft:ash", "Ash (Nether)"}, {"minecraft:white_ash", "White ash (Nether)"},
                {"minecraft:spore_blossom_air", "Spore blossom"}, {"minecraft:cherry_leaves", "Cherry leaves"},
                {"minecraft:bubble", "Bubbles"}, {"minecraft:splash", "Splashes"}};
        for (String[] e : list) IMPORTANT.put(e[0], e[1]);
    }

    private final NumberSetting percent;
    private final StringSetting disabled;
    private final Random random = new Random();
    private volatile Set<String> disabledSet = Set.of();

    public ParticleLimitModule() {
        super("particlelimit", "Particles", "Turn off particles one by one and limit the rest", Category.PERFORMANCE, false);
        add(new ActionSetting("choose", "Choose particles…", () -> platform().openScreen(new ParticleScreen(core, this))));
        percent = add(new NumberSetting("percent", "Keep remaining particles", 100, 0, 100, 5, " %"));
        disabled = add(new StringSetting("disabled", "Disabled", "", 100_000));
        disabled.hidden();
        disabled.onChange(v -> rebuild());
    }


    @Override public void afterLoad() { rebuild(); }

    private void rebuild() {
        String v = disabled.get();
        disabledSet = v.isBlank() ? Set.of() : Collections.unmodifiableSet(new HashSet<>(Arrays.asList(v.split(","))));
    }

    public boolean isDisabled(String id) { return disabledSet.contains(id); }

    public void setDisabled(String id, boolean off) {
        Set<String> next = new TreeSet<>(disabledSet);
        if (off) next.add(id);
        else next.remove(id);
        disabled.set(String.join(",", next));
    }

    /** May a particle of this type appear? (type switched off, otherwise percent limit) */
    public boolean allow(String id) {
        if (!isActive()) return true;
        if (id != null && disabledSet.contains(id)) return false;
        double p = percent.get() / 100.0;
        return p >= 1 || random.nextDouble() < p;
    }

    /** For directly created particles without a type: only the percent limit. */
    public boolean allow() { return allow(null); }
}
