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
import java.util.Set;

/**
 * Partikel: jede Partikelart einzeln abschaltbar (mit Suche und den wichtigsten oben), dazu ein
 * allgemeines Limit, wie viel Prozent der übrigen Partikel erscheinen.
 */
public final class ParticleLimitModule extends Module {
    /** Eigene IDs für Partikel, die Minecraft ohne eigene Partikelart erzeugt. */
    public static final String BLOCK_BREAK = "aio:block_break";
    public static final String BLOCK_CRACK = "aio:block_crack";

    /** Häufig abgeschaltete Partikel mit deutschem Namen (oben in der Liste). */
    public static final Map<String, String> IMPORTANT = new LinkedHashMap<>();

    static {
        String[][] list = {
                {BLOCK_BREAK, "Block zerbrochen (Splitter)"}, {BLOCK_CRACK, "Block abbauen (Splitter beim Hacken)"},
                {"minecraft:block", "Block-Staub (Landen, Sprinten)"}, {"minecraft:explosion", "Explosion"},
                {"minecraft:explosion_emitter", "Große Explosion"}, {"minecraft:poof", "Rauchwolke (Tod von Mobs)"},
                {"minecraft:crit", "Kritischer Treffer"}, {"minecraft:enchanted_hit", "Verzauberter Treffer"},
                {"minecraft:damage_indicator", "Schadensanzeige (Herzen)"}, {"minecraft:sweep_attack", "Schwungangriff"},
                {"minecraft:totem_of_undying", "Totem der Unsterblichkeit"}, {"minecraft:entity_effect", "Trank-Effekte"},
                {"minecraft:firework", "Feuerwerk"}, {"minecraft:campfire_cosy_smoke", "Lagerfeuer-Rauch"},
                {"minecraft:campfire_signal_smoke", "Signalfeuer-Rauch"}, {"minecraft:smoke", "Rauch"},
                {"minecraft:large_smoke", "Großer Rauch"}, {"minecraft:rain", "Regentropfen"},
                {"minecraft:portal", "Portal"}, {"minecraft:heart", "Herzen (Zucht)"},
                {"minecraft:angry_villager", "Wütender Dorfbewohner"}, {"minecraft:happy_villager", "Glücklicher Dorfbewohner"},
                {"minecraft:dust", "Redstone-Staub"}, {"minecraft:falling_dust", "Fallender Staub"},
                {"minecraft:ash", "Asche (Nether)"}, {"minecraft:white_ash", "Weiße Asche (Nether)"},
                {"minecraft:spore_blossom_air", "Sporenblüte"}, {"minecraft:cherry_leaves", "Kirschblätter"},
                {"minecraft:bubble", "Blasen"}, {"minecraft:splash", "Spritzer"}};
        for (String[] e : list) IMPORTANT.put(e[0], e[1]);
    }

    private final NumberSetting percent;
    private final StringSetting disabled;
    private final java.util.Random random = new java.util.Random();
    private volatile Set<String> disabledSet = Set.of();

    public ParticleLimitModule() {
        super("particlelimit", "Partikel", "Partikel einzeln abschalten und begrenzen", Category.PERFORMANCE, false);
        add(new ActionSetting("choose", "Partikel auswählen…", () -> platform().openScreen(new ParticleScreen(core, this))));
        percent = add(new NumberSetting("percent", "Übrige Partikel behalten", 100, 0, 100, 5, " %"));
        disabled = add(new StringSetting("disabled", "Abgeschaltet", "", 100_000));
        disabled.hidden();
        disabled.onChange(v -> rebuild());
    }

    @Override public boolean enabledByDefault() { return true; }

    @Override public void afterLoad() { rebuild(); }

    private void rebuild() {
        String v = disabled.get();
        disabledSet = v.isBlank() ? Set.of() : Collections.unmodifiableSet(new HashSet<>(Arrays.asList(v.split(","))));
    }

    public boolean isDisabled(String id) { return disabledSet.contains(id); }

    public void setDisabled(String id, boolean off) {
        Set<String> next = new java.util.TreeSet<>(disabledSet);
        if (off) next.add(id);
        else next.remove(id);
        disabled.set(String.join(",", next));
    }

    /** Darf ein Partikel dieser Art erscheinen? (Art abgeschaltet, sonst Prozent-Limit) */
    public boolean allow(String id) {
        if (!isActive()) return true;
        if (id != null && disabledSet.contains(id)) return false;
        double p = percent.get() / 100.0;
        return p >= 1 || random.nextDouble() < p;
    }

    /** Für direkt erzeugte Partikel ohne Art: nur das Prozent-Limit. */
    public boolean allow() { return allow(null); }
}
