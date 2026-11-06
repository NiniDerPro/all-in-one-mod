package dev.aio.core.modules.map;

import dev.aio.core.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Gemerkte Oberfläche der Welt für die Karte: je Chunk 256 Säulen mit Farbe und Höhe, gespeichert
 * pro Welt und Dimension (gzip, eine Datei). Unerforschtes fehlt einfach und bleibt dunkel.
 */
public final class MapStore {
    static final class Tile {
        final int[] rgb = new int[256];
        final short[] h = new short[256];
    }

    private final Path root;
    private final Map<Long, Tile> tiles = new HashMap<>();
    private String key = "";
    private boolean dirty;
    /** Zählt jede Änderung, damit die Minimap weiß, wann sie neu zeichnen muss. */
    public int version;

    public MapStore(Path root) { this.root = root; }

    private static long k(int cx, int cz) { return ((long) cx << 32) | (cz & 0xFFFFFFFFL); }

    /** 0 = unbekannt, sonst ARGB. */
    public int rgb(int x, int z) {
        Tile t = tiles.get(k(x >> 4, z >> 4));
        return t == null ? 0 : t.rgb[(z & 15) * 16 + (x & 15)];
    }

    public int height(int x, int z) {
        Tile t = tiles.get(k(x >> 4, z >> 4));
        return t == null ? 0 : t.h[(z & 15) * 16 + (x & 15)];
    }

    public void put(int x, int z, int argb, int height) {
        Tile t = tiles.computeIfAbsent(k(x >> 4, z >> 4), id -> new Tile());
        int i = (z & 15) * 16 + (x & 15);
        if (t.rgb[i] != argb || t.h[i] != height) {
            t.rgb[i] = argb;
            t.h[i] = (short) height;
            dirty = true;
            version++;
        }
    }

    /** Auf eine andere Welt/Dimension wechseln: alte sichern, neue laden. */
    public void switchTo(String world, String dimension) {
        String next = world + "|" + dimension;
        if (next.equals(key)) return;
        save();
        tiles.clear();
        version++;
        key = next;
        load();
    }

    private Path file() {
        String w = key.substring(0, Math.max(0, key.indexOf('|'))).replaceAll("[^A-Za-z0-9._-]", "_");
        String d = key.substring(key.indexOf('|') + 1).replaceAll("[^A-Za-z0-9._-]", "_");
        return root.resolve(w.isEmpty() ? "world" : w).resolve(d + ".bin");
    }

    public void save() {
        if (!dirty || key.isEmpty()) return;
        dirty = false;
        Path f = file();
        try {
            Files.createDirectories(f.getParent());
            Path tmp = f.resolveSibling(f.getFileName() + ".tmp");
            try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(tmp)))) {
                out.writeInt(tiles.size());
                for (Map.Entry<Long, Tile> e : tiles.entrySet()) {
                    out.writeInt((int) (e.getKey() >> 32));
                    out.writeInt((int) (long) e.getKey());
                    for (int v : e.getValue().rgb) out.writeInt(v);
                    for (short v : e.getValue().h) out.writeShort(v);
                }
            }
            Files.move(tmp, f, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Log.error("Karte konnte nicht gespeichert werden", e);
        }
    }

    private void load() {
        Path f = file();
        if (!Files.isRegularFile(f)) return;
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(Files.newInputStream(f)))) {
            int n = in.readInt();
            for (int i = 0; i < n; i++) {
                int cx = in.readInt(), cz = in.readInt();
                Tile t = new Tile();
                for (int j = 0; j < 256; j++) t.rgb[j] = in.readInt();
                for (int j = 0; j < 256; j++) t.h[j] = in.readShort();
                tiles.put(k(cx, cz), t);
            }
        } catch (IOException | RuntimeException e) {
            Log.error("Karte defekt, beginne neu", e);
            tiles.clear();
        }
    }
}
