package dev.aio.core.ui;

import dev.aio.core.module.Module;
import dev.aio.core.setting.EnumSetting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Sorting of the module cards in the menu. */
public enum SortMode implements EnumSetting.Named {
    DEFAULT("Default"), ALPHABETICAL("A–Z"), ACTIVE_FIRST("Active first");

    private final String displayName;

    SortMode(String displayName) { this.displayName = displayName; }

    @Override public String displayName() { return displayName; }

    public List<Module> sort(List<Module> modules) {
        List<Module> out = new ArrayList<>(modules);
        Comparator<Module> byName = Comparator.comparing(m -> m.name().toLowerCase(Locale.ROOT));
        switch (this) {
            case ALPHABETICAL -> out.sort(byName);
            case ACTIVE_FIRST -> out.sort(Comparator.comparing((Module m) -> !m.isEnabled()).thenComparing(byName));
            case DEFAULT -> { }
        }
        return out;
    }

    /** Filters by name, description and category (all words must match). */
    public static List<Module> search(List<Module> modules, String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return modules;
        String[] words = q.split("\\s+");
        List<Module> out = new ArrayList<>();
        for (Module m : modules) {
            String hay = (m.name() + " " + m.description() + " " + m.category().displayName()).toLowerCase(Locale.ROOT);
            // "nofog" also finds "No Fog": ignore spaces, search the id too
            String compact = hay.replace(" ", "") + " " + m.id();
            boolean all = true;
            for (String w : words) if (!hay.contains(w) && !compact.contains(w)) { all = false; break; }
            if (all) out.add(m);
        }
        return out;
    }
}
