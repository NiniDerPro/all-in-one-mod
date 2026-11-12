package dev.aio.core.preview;

import dev.aio.core.platform.ItemRef;
import dev.aio.core.platform.Renderer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;

/**
 * Draws the interface with Java2D into an image, with the real Minecraft font and real
 * textures from the game jar. Only for design previews (tests), not in the game.
 */
public final class PreviewRenderer implements Renderer {
    private final int guiW, guiH, scale;
    private final BufferedImage image;
    private final Graphics2D g;
    private final Deque<AffineTransform> stack = new ArrayDeque<>();
    private final ZipFile jar;
    private final Map<Integer, Glyph> glyphs = new HashMap<>();
    private final Map<String, BufferedImage> textures = new HashMap<>();

    private record Glyph(BufferedImage sheet, int sx, int sy, int cell, int height, int width, int ascent) {}

    public PreviewRenderer(int guiW, int guiH, int scale, File clientJar) throws IOException {
        this.guiW = guiW;
        this.guiH = guiH;
        this.scale = scale;
        image = new BufferedImage(guiW * scale, guiH * scale, BufferedImage.TYPE_INT_ARGB);
        g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.scale(scale, scale);
        jar = clientJar != null && clientJar.exists() ? new ZipFile(clientJar) : null;
        if (jar != null) loadFont();
    }

    /** Background, e.g. a screenshot from the game or a colour. */
    public void background(int argb) {
        AffineTransform t = g.getTransform();
        g.setTransform(new AffineTransform());
        g.setColor(new Color(argb, true));
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.setTransform(t);
    }

    /** Pseudo game scene as background: sky, horizon, grass, a few blocks. */
    public void fakeWorld() {
        AffineTransform t = g.getTransform();
        g.setTransform(new AffineTransform());
        int w = image.getWidth(), h = image.getHeight();
        for (int y = 0; y < h * 0.55; y++) {
            float f = y / (float) (h * 0.55);
            g.setColor(new Color(lerp(0x7FA9FF, 0xC4DCFF, f)));
            g.fillRect(0, y, w, 1);
        }
        BufferedImage grass = texture("assets/minecraft/textures/block/grass_block_top.png");
        BufferedImage dirt = texture("assets/minecraft/textures/block/dirt.png");
        int tile = 16 * scale * 2;
        for (int y = (int) (h * 0.55); y < h; y += tile) {
            for (int x = 0; x < w; x += tile) {
                BufferedImage tex = y < h * 0.55 + tile ? grass : dirt;
                if (tex != null) {
                    g.drawImage(tex, x, y, tile, tile, null);
                    if (tex == grass) {
                        g.setColor(new Color(0x80, 0xB8, 0x4A, 140));
                        g.fillRect(x, y, tile, tile);
                    }
                } else {
                    g.setColor(new Color(0x5A8F3C));
                    g.fillRect(x, y, tile, tile);
                }
            }
        }
        g.setTransform(t);
    }

    private static int lerp(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int gg = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bb = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        return r << 16 | gg << 8 | bb;
    }

    public void save(File file) throws IOException {
        file.getParentFile().mkdirs();
        ImageIO.write(image, "png", file);
    }

    // ---- Schrift ----

    private void loadFont() throws IOException {
        JsonObject def = json("assets/minecraft/font/include/default.json");
        if (def == null) return;
        for (JsonElement p : def.getAsJsonArray("providers")) {
            JsonObject o = p.getAsJsonObject();
            if (!"bitmap".equals(o.get("type").getAsString())) continue;
            String file = o.get("file").getAsString().replace("minecraft:", "");
            BufferedImage sheet = texture("assets/minecraft/textures/" + file);
            if (sheet == null) continue;
            int height = o.has("height") ? o.get("height").getAsInt() : 8;
            int ascent = o.get("ascent").getAsInt();
            JsonArray rows = o.getAsJsonArray("chars");
            int cols = rows.get(0).getAsString().codePointCount(0, rows.get(0).getAsString().length());
            int cellW = sheet.getWidth() / cols, cellH = sheet.getHeight() / rows.size();
            for (int ry = 0; ry < rows.size(); ry++) {
                String row = rows.get(ry).getAsString();
                int cx = 0;
                for (int i = 0; i < row.length(); ) {
                    int cp = row.codePointAt(i);
                    i += Character.charCount(cp);
                    if (cp != 0 && !glyphs.containsKey(cp)) {
                        int sx = cx * cellW, sy = ry * cellH;
                        int last = -1;
                        for (int x = cellW - 1; x >= 0 && last < 0; x--) {
                            for (int y = 0; y < cellH; y++) {
                                if ((sheet.getRGB(sx + x, sy + y) >>> 24) != 0) {
                                    last = x;
                                    break;
                                }
                            }
                        }
                        float pxScale = height / (float) cellH;
                        int width = cp == ' ' ? 4 : (int) Math.floor((last + 1) * pxScale) + 1;
                        glyphs.put(cp, new Glyph(sheet, sx, sy, cellW, cellH, width, ascent));
                    }
                    cx++;
                }
            }
        }
    }

    private JsonObject json(String path) throws IOException {
        ZipEntry e = jar.getEntry(path);
        if (e == null) return null;
        try (InputStream in = jar.getInputStream(e)) {
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private BufferedImage texture(String path) {
        return textures.computeIfAbsent(path, p -> {
            if (jar == null) return null;
            try {
                ZipEntry e = jar.getEntry(p);
                if (e == null) return null;
                try (InputStream in = jar.getInputStream(e)) {
                    BufferedImage img = ImageIO.read(in);
                    if (img != null && img.getHeight() > img.getWidth()) img = img.getSubimage(0, 0, img.getWidth(), img.getWidth());
                    return img;
                }
            } catch (IOException ex) {
                return null;
            }
        });
    }

    @Override public int screenWidth() { return guiW; }

    @Override public int screenHeight() { return guiH; }

    @Override public int textWidth(String text) {
        int w = 0;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            if (cp == '§' && i < text.length()) {
                i++;
                continue;
            }
            Glyph gl = glyphs.get(cp);
            w += gl != null ? gl.width : 6;
        }
        return w;
    }

    @Override public int fontHeight() { return 9; }

    @Override public void text(String text, float x, float y, int argb, boolean shadow) {
        if (shadow) drawText(text, x + 1, y + 1, shadowColor(argb));
        drawText(text, x, y, argb);
    }

    private static int shadowColor(int argb) {
        int a = argb >>> 24;
        int r = ((argb >> 16) & 255) / 4, gg = ((argb >> 8) & 255) / 4, b = (argb & 255) / 4;
        return a << 24 | r << 16 | gg << 8 | b;
    }

    private void drawText(String text, float x, float y, int argb) {
        float cx = x;
        int color = argb;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            if (cp == '§' && i < text.length()) {
                char code = text.charAt(i++);
                Integer c = formatColor(code);
                if (c != null) color = (argb & 0xFF000000) | c;
                continue;
            }
            Glyph gl = glyphs.get(cp);
            if (gl == null) {
                fill(cx, y + 1, cx + 5, y + 8, color);
                cx += 6;
                continue;
            }
            if (cp != ' ') {
                BufferedImage tinted = tint(gl, color);
                int dh = Math.round(gl.height * (8f / gl.cell));
                AffineTransform t = g.getTransform();
                g.translate(cx, y + 7 - gl.ascent);
                g.scale(8f / gl.cell, dh / (float) gl.height);
                g.drawImage(tinted, 0, 0, null);
                g.setTransform(t);
            }
            cx += gl.width;
        }
    }

    private final Map<Long, BufferedImage> tintCache = new HashMap<>();

    private BufferedImage tint(Glyph gl, int argb) {
        long key = ((long) System.identityHashCode(gl.sheet) << 40) ^ ((long) gl.sx << 28) ^ ((long) gl.sy << 16) ^ (argb & 0xFFFFFFFFL) * 31;
        return tintCache.computeIfAbsent(key, k -> {
            BufferedImage out = new BufferedImage(gl.cell, gl.height, BufferedImage.TYPE_INT_ARGB);
            int a = argb >>> 24;
            for (int x = 0; x < gl.cell; x++) {
                for (int y = 0; y < gl.height; y++) {
                    int px = gl.sheet.getRGB(gl.sx + x, gl.sy + y);
                    int pa = px >>> 24;
                    if (pa == 0) continue;
                    out.setRGB(x, y, ((pa * a / 255) << 24) | (argb & 0xFFFFFF));
                }
            }
            return out;
        });
    }

    private static Integer formatColor(char c) {
        String codes = "0123456789abcdef";
        int[] colors = {0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
                0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};
        int i = codes.indexOf(Character.toLowerCase(c));
        return i < 0 ? null : colors[i];
    }

    // ---- Formen ----

    @Override public void fill(float x1, float y1, float x2, float y2, int argb) {
        if ((argb >>> 24) == 0) return;
        g.setColor(new Color(argb, true));
        g.fill(new Rectangle2D.Float(Math.round(x1), Math.round(y1), Math.round(x2) - Math.round(x1), Math.round(y2) - Math.round(y1)));
    }

    @Override public void item(ItemRef item, float x, float y, boolean overlay) {
        if (item == null || item.isEmpty()) return;
        String id = item.id().isEmpty() ? item.name() : item.id();
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        BufferedImage tex = texture("assets/minecraft/textures/item/" + path + ".png");
        if (tex == null) tex = texture("assets/minecraft/textures/block/" + path + ".png");
        if (tex != null) {
            AffineTransform t = g.getTransform();
            g.translate(x, y);
            g.scale(16f / tex.getWidth(), 16f / tex.getHeight());
            g.drawImage(tex, 0, 0, null);
            g.setTransform(t);
        } else {
            fill(x + 2, y + 2, x + 14, y + 14, 0xFF8A8A9A);
        }
        if (overlay && item.isDamageable() && item.damage() > 0) {
            float f = item.durabilityFraction();
            fill(x + 2, y + 13, x + 15, y + 15, 0xFF000000);
            fill(x + 2, y + 13, x + 2 + 13 * f, y + 14, 0xFF000000 | dev.aio.core.util.Colors.health(f) & 0xFFFFFF);
        }
        if (overlay && item.count() > 1) {
            String c = String.valueOf(item.count());
            text(c, x + 17 - textWidth(c), y + 9, 0xFFFFFFFF, true);
        }
    }

    @Override public void sprite(String id, float x, float y, int width, int height) {
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        BufferedImage tex = texture("assets/minecraft/textures/gui/sprites/" + path + ".png");
        if (tex == null) return;
        AffineTransform t = g.getTransform();
        g.translate(x, y);
        g.scale(width / (float) tex.getWidth(), height / (float) tex.getHeight());
        g.drawImage(tex, 0, 0, null);
        g.setTransform(t);
    }

    @Override public void push() { stack.push(g.getTransform()); }

    @Override public void pop() { if (!stack.isEmpty()) g.setTransform(stack.pop()); }

    @Override public void translate(float x, float y) { g.translate(x, y); }

    @Override public void scale(float factor) { g.scale(factor, factor); }

    @Override public void scale(float sx, float sy) { g.scale(sx, sy); }

    private Shape savedClip;

    @Override public void enableScissor(int x1, int y1, int x2, int y2) {
        AffineTransform t = g.getTransform();
        g.setTransform(AffineTransform.getScaleInstance(scale, scale));
        g.setClip(new Rectangle2D.Float(x1, y1, x2 - x1, y2 - y1));
        g.setTransform(t);
    }

    @Override public void disableScissor() { g.setClip(null); }
}
