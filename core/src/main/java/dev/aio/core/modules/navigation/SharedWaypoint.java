package dev.aio.core.modules.navigation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Recognises waypoints shared in chat. Supports the text format that Xaero's Minimap
 * sent when sharing:
 * {@code xaero-waypoint:Name:N:X:Y:Z:Colour:false:0:Internal-overworld-waypoints}
 * (Y can be {@code ~}, a colon in the name is written as {@code ^col^}). Nothing here is
 * taken from other mods, it only follows the visible text format.
 */
public final class SharedWaypoint {
    /** Colours of the 16 Minecraft colour codes, as Xaero sends them as index 0-15. */
    private static final int[] COLORS = {
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};

    private static final Pattern XAERO = Pattern.compile("xaero[-_]waypoint:([^\\s]+)");

    public record Found(String name, int x, Integer y, int z, int color, String dimension) {
        /** Short form for the click command: name|x|y|z|colour|dimension (y empty = current height). */
        public String payload() {
            return name.replace("|", " ") + "|" + x + "|" + (y == null ? "" : y) + "|" + z + "|" + Integer.toHexString(color) + "|" + (dimension == null ? "" : dimension);
        }

        public static Found fromPayload(String s) {
            String[] p = s.split("\\|", -1);
            if (p.length < 6) return null;
            try {
                return new Found(p[0], Integer.parseInt(p[1]), p[2].isEmpty() ? null : Integer.valueOf(p[2]), Integer.parseInt(p[3]),
                        Integer.parseInt(p[4], 16), p[5].isEmpty() ? null : p[5]);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    /** A hit including the text position so that the message can be rewritten. */
    public record Match(Found waypoint, int start, int end) {}

    public static List<Match> find(String text) {
        List<Match> out = new ArrayList<>();
        if (text == null) return out;
        Matcher m = XAERO.matcher(text);
        while (m.find()) {
            Found f = one(m.group(1));
            if (f != null) out.add(new Match(f, m.start(), m.end()));
        }
        return out;
    }

    private SharedWaypoint() {}

    /** All waypoints in a chat message (usually null or one). */
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
