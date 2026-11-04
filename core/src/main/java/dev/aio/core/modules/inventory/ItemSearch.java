package dev.aio.core.modules.inventory;

import dev.aio.core.platform.ItemRef;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Suche in der Item-Liste. Mehrere Wörter = alle müssen passen.
 * {@code @mod} filtert nach Mod (Name oder ID-Namespace), {@code $id} nach Registry-ID,
 * sonst wird im Namen und in der ID gesucht. {@code -wort} schließt aus.
 */
public final class ItemSearch {
    private ItemSearch() {}

    /**
     * Suchindex: Namen, IDs und Mods einmal kleingeschrieben vorberechnet. Wird die Suche nur
     * verlängert ("dia" → "diam"), wird nur in den bisherigen Treffern weitergesucht.
     */
    public static final class Index {
        private record Entry(ItemRef item, String name, String id, String ns, String mod) {}

        private final List<Entry> entries;
        private final List<ItemRef> all;
        private String lastQuery = "";
        private List<Entry> lastHits;

        public Index(List<ItemRef> items) {
            all = items;
            entries = new ArrayList<>(items.size());
            for (ItemRef it : items) {
                String id = it.id().toLowerCase(Locale.ROOT);
                String ns = id.contains(":") ? id.substring(0, id.indexOf(':')) : "minecraft";
                entries.add(new Entry(it, it.name().toLowerCase(Locale.ROOT), id, ns,
                        it.modName().toLowerCase(Locale.ROOT).replace(" ", "")));
            }
            lastHits = entries;
        }

        public List<ItemRef> filter(String query) {
            String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
            if (q.isEmpty()) {
                lastQuery = "";
                lastHits = entries;
                return all;
            }
            String[] tokens = q.split("\\s+");
            // Nur verlängert und ohne Ausschluss: Treffer können nur weniger werden
            boolean narrowing = !lastQuery.isEmpty() && q.startsWith(lastQuery) && !q.contains("-");
            List<Entry> source = narrowing ? lastHits : entries;
            List<Entry> hits = new ArrayList<>();
            for (Entry e : source) if (matchesAll(e, tokens)) hits.add(e);
            lastQuery = q;
            lastHits = hits;
            List<ItemRef> out = new ArrayList<>(hits.size());
            for (Entry e : hits) out.add(e.item);
            return out;
        }

        private static boolean matchesAll(Entry e, String[] tokens) {
            for (String t : tokens) {
                boolean negate = t.startsWith("-") && t.length() > 1;
                String tok = negate ? t.substring(1) : t;
                if (matches(e, tok) == negate) return false;
            }
            return true;
        }

        private static boolean matches(Entry e, String tok) {
            if (tok.startsWith("@")) {
                String m = tok.substring(1);
                return m.isEmpty() || e.ns.contains(m) || e.mod.contains(m);
            }
            if (tok.startsWith("$")) return e.id.contains(tok.substring(1));
            return e.name.contains(tok) || e.id.contains(tok);
        }
    }

    public static List<ItemRef> filter(List<ItemRef> items, String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return items;
        String[] tokens = q.split("\\s+");
        List<ItemRef> out = new ArrayList<>();
        for (ItemRef it : items) if (matchesAll(it, tokens)) out.add(it);
        return out;
    }

    static boolean matchesAll(ItemRef it, String[] tokens) {
        for (String t : tokens) {
            boolean negate = t.startsWith("-") && t.length() > 1;
            String tok = negate ? t.substring(1) : t;
            if (matches(it, tok) == negate) return false;
        }
        return true;
    }

    static boolean matches(ItemRef it, String tok) {
        String id = it.id().toLowerCase(Locale.ROOT);
        if (tok.startsWith("@")) {
            String m = tok.substring(1);
            if (m.isEmpty()) return true;
            String ns = id.contains(":") ? id.substring(0, id.indexOf(':')) : "minecraft";
            return ns.contains(m) || it.modName().toLowerCase(Locale.ROOT).replace(" ", "").contains(m);
        }
        if (tok.startsWith("$")) return id.contains(tok.substring(1));
        return it.name().toLowerCase(Locale.ROOT).contains(tok) || id.contains(tok);
    }
}
