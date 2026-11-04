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
