package dev.aio.core.modules.navigation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Erkennt Wegpunkte, die im Chat geteilt werden. Unterstützt das Textformat, das Xaero's Minimap
 * beim Teilen verschickt:
 * {@code xaero-waypoint:Name:N:X:Y:Z:Farbe:false:0:Internal-overworld-waypoints}
 * (Y kann {@code ~} sein, im Namen steht ein Doppelpunkt als {@code ^col^}). Nichts hier ist
 * von anderen Mods übernommen, es folgt nur dem sichtbaren Textformat.
 */
public final class SharedWaypoint {
    /** Farben der 16 Minecraft-Farbcodes, wie Xaero sie als Index 0-15 verschickt. */
    private static final int[] COLORS = {
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};

    private static final Pattern XAERO = Pattern.compile("xaero[-_]waypoint:([^\\s]+)");

    public record Found(String name, int x, Integer y, int z, int color, String dimension) {}

    private SharedWaypoint() {}

    /** Alle Wegpunkte in einer Chatnachricht (meist null oder einer). */
    public static List<Found> parse(String text) {
        List<Found> out = new ArrayList<>();
        if (text == null) return out;
        Matcher m = XAERO.matcher(text);
        while (m.find()) {
            Found f = one(m.group(1));
            if (f != null) out.add(f);
        }
        return out;
    }

    private static Found one(String body) {
        String[] p = body.split(":", -1);
        if (p.length < 6) return null;
        try {
            String name = p[0].replace("^col^", ":").replace("§§", ":").replace('_', ' ');
            int x = Integer.parseInt(p[2].trim());
            Integer y = p[3].equals("~") ? null : Integer.valueOf(p[3].trim());
            int z = Integer.parseInt(p[4].trim());
            int ci = Integer.parseInt(p[5].trim());
            int color = ci >= 0 && ci < COLORS.length ? COLORS[ci] : 0x55AAFF;
            return new Found(name.isBlank() ? "Shared" : name.trim(), x, y, z, color, dimension(p[p.length - 1]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** "Internal-the_nether-waypoints" → "the_nether"; unbekannt → null (aktuelle Dimension). */
    private static String dimension(String set) {
        String s = set.toLowerCase();
        if (s.contains("nether")) return "the_nether";
        if (s.contains("the_end") || s.contains("-end-")) return "the_end";
        if (s.contains("overworld")) return "overworld";
        return null;
    }
}
