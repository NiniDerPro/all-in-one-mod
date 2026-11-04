package dev.aio.core.ui;

import dev.aio.core.module.Module;
import dev.aio.core.setting.EnumSetting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Sortierung der Modulkarten im Menü. */
public enum SortMode implements EnumSetting.Named {
    DEFAULT("Standard"), ALPHABETICAL("A–Z"), ACTIVE_FIRST("Aktive zuerst");

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

    /** Filtert nach Name, Beschreibung und Kategorie (alle Wörter müssen passen). */
    public static List<Module> search(List<Module> modules, String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return modules;
        String[] words = q.split("\\s+");
        List<Module> out = new ArrayList<>();
        for (Module m : modules) {
            String hay = (m.name() + " " + m.description() + " " + m.category().displayName()).toLowerCase(Locale.ROOT);
            boolean all = true;
            for (String w : words) if (!hay.contains(w)) { all = false; break; }
            if (all) out.add(m);
        }
        return out;
    }
}
