package dev.aio.core.modules.client;

import dev.aio.core.AioCore;
import dev.aio.core.module.Module;
import dev.aio.core.platform.KeyBinding;
import dev.aio.core.platform.Keys;
import dev.aio.core.setting.KeybindSetting;
import dev.aio.core.setting.Setting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Alle Tastenbelegungen (Minecraft, Mods und AIO) mit Suche, Filtern, Sortierung und Konflikten.
 * Ohne Minecraft-Abhängigkeit, damit es testbar ist.
 */
public final class KeyBindIndex {
    public static final String AIO_PREFIX = "aio:";

    public enum Filter { ALL("All"), CONFLICTS("Conflicts"), UNBOUND("Unbound"), FREE("Free keys");
        public final String label;
        Filter(String label) { this.label = label; }
    }

    public enum Sort { DEFAULT("Default"), NAME_AZ("Name A–Z"), NAME_ZA("Name Z–A"), KEY_AZ("Key A–Z");
        public final String label;
        Sort(String label) { this.label = label; }
    }

    /** Eine Zeile: Belegung plus Anzeigename der Taste. */
    public record Entry(KeyBinding binding, String keyDisplay, boolean conflict) {}

    private final AioCore core;
    private List<KeyBinding> bindings = List.of();
    private final Map<String, Integer> keyUse = new HashMap<>();

    public KeyBindIndex(AioCore core) { this.core = core; }

    /** Liest alle Belegungen neu (nach jeder Änderung). */
    public void refresh() {
        List<KeyBinding> list = new ArrayList<>(core.platform().keyBindings());
        for (Module m : core.modules().all()) {
            for (Setting<?> s : m.settings()) {
                if (s instanceof KeybindSetting k && !(k == m.toggleKey() && !m.canToggle()) && !k.isHidden()) {
                    list.add(new KeyBinding(AIO_PREFIX + m.id() + "." + k.id(), m.name() + ": " + k.name(),
                            "AIO ClientSide", k.key(), k.defaultValue()));
                }
            }
        }
        bindings = list;
        keyUse.clear();
        for (KeyBinding b : bindings) if (!b.unbound()) keyUse.merge(b.key(), 1, Integer::sum);
    }

    public List<KeyBinding> bindings() { return bindings; }

    public boolean conflict(KeyBinding b) { return !b.unbound() && keyUse.getOrDefault(b.key(), 0) > 1; }

    /** Gefilterte und sortierte Einträge; die Suche prüft Name, Kategorie und Taste. */
    public List<Entry> entries(String query, Filter filter, Sort sort) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Entry> out = new ArrayList<>();
        for (KeyBinding b : bindings) {
            boolean c = conflict(b);
            if (filter == Filter.CONFLICTS && !c) continue;
            if (filter == Filter.UNBOUND && !b.unbound()) continue;
            String display = b.unbound() ? "" : core.platform().keyName(b.key());
            if (!q.isEmpty() && !matches(q, b, display)) continue;
            out.add(new Entry(b, display, c));
        }
        Comparator<Entry> cmp = switch (sort) {
            case DEFAULT -> null;
            case NAME_AZ -> Comparator.comparing(e -> e.binding.name().toLowerCase(Locale.ROOT));
            case NAME_ZA -> Comparator.<Entry, String>comparing(e -> e.binding.name().toLowerCase(Locale.ROOT)).reversed();
            case KEY_AZ -> Comparator.comparing((Entry e) -> e.binding.unbound())
                    .thenComparing(e -> e.keyDisplay.toLowerCase(Locale.ROOT));
        };
        if (cmp != null) out.sort(cmp);
        return out;
    }

    private static boolean matches(String q, KeyBinding b, String display) {
        return b.name().toLowerCase(Locale.ROOT).contains(q)
                || b.category().toLowerCase(Locale.ROOT).contains(q)
                || display.toLowerCase(Locale.ROOT).equals(q)
                || display.toLowerCase(Locale.ROOT).startsWith(q);
    }

    /** Tastaturtasten, die keiner Belegung zugeordnet sind. */
    public List<String> freeKeys() {
        List<String> out = new ArrayList<>();
        for (String k : KEYBOARD) if (!keyUse.containsKey(k)) out.add(k);
        return out;
    }

    /** Setzt eine Belegung; AIO-Tasten landen in den Moduleinstellungen. */
    public void set(KeyBinding b, String key) {
        String k = key == null ? Keys.NONE : key;
        if (b.id().startsWith(AIO_PREFIX)) {
            String rest = b.id().substring(AIO_PREFIX.length());
            int dot = rest.indexOf('.');
            Module m = core.modules().get(rest.substring(0, dot));
            if (m != null && m.setting(rest.substring(dot + 1)) instanceof KeybindSetting ks) ks.set(k);
        } else {
            core.platform().setKeyBinding(b.id(), k);
        }
        refresh();
    }

    public void resetAll() {
        for (KeyBinding b : new ArrayList<>(bindings)) if (!b.isDefault()) set(b, b.defaultKey());
    }

    /** Übliche Tastaturtasten als MC-Namen (für "Freie Tasten"). */
    public static final List<String> KEYBOARD;

    static {
        Set<String> k = new LinkedHashSet<>();
        for (char c = 'a'; c <= 'z'; c++) k.add("key.keyboard." + c);
        for (int i = 0; i <= 9; i++) k.add("key.keyboard." + i);
        for (int i = 1; i <= 12; i++) k.add("key.keyboard.f" + i);
        for (int i = 0; i <= 9; i++) k.add("key.keyboard.keypad." + i);
        for (String s : new String[]{"keypad.add", "keypad.subtract", "keypad.multiply", "keypad.divide", "keypad.decimal",
                "keypad.enter", "grave.accent", "minus", "equal", "left.bracket", "right.bracket", "backslash", "semicolon",
                "apostrophe", "comma", "period", "slash", "tab", "caps.lock", "left.shift", "right.shift", "left.control",
                "right.control", "left.alt", "right.alt", "space", "enter", "backspace", "insert", "delete", "home", "end",
                "page.up", "page.down", "up", "down", "left", "right", "pause", "scroll.lock", "print.screen", "menu"}) {
            k.add("key.keyboard." + s);
        }
        KEYBOARD = List.copyOf(k);
    }
}
