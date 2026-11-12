package dev.aio.core.modules.navigation;

import dev.aio.core.i18n.Lang;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aio.core.AioCore;
import dev.aio.core.platform.Keys;
import dev.aio.core.platform.Renderer;
import dev.aio.core.ui.TextEdit;
import dev.aio.core.ui.Theme;
import dev.aio.core.util.Colors;
import dev.aio.core.ui.Draw;
import dev.aio.core.ui.Window;
import dev.aio.core.ui.UiScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Small window for creating (key B) or editing a waypoint: name, colour and
 * coordinates (prefilled with the current position, changeable). Via "Templates" templates
 * (name + colour) can be applied, own ones saved and deleted.
 */
public final class WaypointEditScreen implements UiScreen {
    private record Hit(float x1, float y1, float x2, float y2, IntConsumer action) {}

    /** Template for a waypoint. */
    public record Template(String name, int color) {}

    static final String TEMPLATE_FILE = "waypoint_templates.json";
    static final List<Template> DEFAULT_TEMPLATES = List.of(
            new Template("Home", 0x55FF55), new Template("Base", 0x5599FF), new Template("Farm", 0xFFFF55),
            new Template("Mine", 0xFFAA00), new Template("Portal", 0xAA55FF), new Template("Spawner", 0xFF5555),
            new Template("Village", 0x55FFFF), new Template("Shop", 0xFF55FF));

    private static final String[] LABELS = {Lang.tr("aio.ui.waypoint_edit.name", "Name"), "X", "Y", "Z"};

    private final AioCore core;
    private final WaypointsModule module;
    /** null = neuer Wegpunkt. */
    private final Waypoint original;
    private final String[] fields = new String[4];
    private int focus = 0;
    private boolean selectAll = true;
    private int color;
    private boolean showTemplates;
    private List<Template> templates;
    private final List<Hit> hits = new ArrayList<>();
    private String error;
    /** The "b" of the key that opened the window must not end up in the name. */
    private final long openedAt = System.currentTimeMillis();

    public WaypointEditScreen(AioCore core, WaypointsModule module, Waypoint original) {
        this.core = core;
        this.module = module;
        this.original = original;
        if (original != null) {
            fields[0] = original.name();
            fields[1] = String.valueOf(original.x());
            fields[2] = String.valueOf(original.y());
            fields[3] = String.valueOf(original.z());
            color = original.color();
        } else {
            var p = core.platform().playerPos();
            fields[0] = Lang.tr("aio.ui.waypoint_edit.default_name", "Waypoint %s", module.inThisWorld().size() + 1);
            fields[1] = String.valueOf((int) Math.floor(p.x()));
            fields[2] = String.valueOf((int) Math.floor(p.y()));
            fields[3] = String.valueOf((int) Math.floor(p.z()));
            color = WaypointsModule.PALETTE[module.inThisWorld().size() % WaypointsModule.PALETTE.length];
        }
    }

    @Override public String title() { return original == null ? Lang.tr("aio.ui.waypoint_edit.title_new", "New waypoint")
            : Lang.tr("aio.ui.waypoint_edit.title_edit", "Edit waypoint"); }

    @Override public boolean returnsToPrevious() { return true; }

    // ---- Vorlagen ----

    private List<Template> templates() {
        if (templates != null) return templates;
        templates = new ArrayList<>();
        String json = core.config().readText(TEMPLATE_FILE);
        if (json == null) {
            templates.addAll(DEFAULT_TEMPLATES);
            return templates;
        }
        try {
            for (JsonElement e : JsonParser.parseString(json).getAsJsonArray()) {
                JsonObject o = e.getAsJsonObject();
                templates.add(new Template(o.get("name").getAsString(), o.get("color").getAsInt()));
            }
        } catch (RuntimeException ex) {
            templates.clear();
            templates.addAll(DEFAULT_TEMPLATES);
        }
        return templates;
    }

    private void saveTemplates() {
        JsonArray arr = new JsonArray();
        for (Template t : templates()) {
            JsonObject o = new JsonObject();
            o.addProperty("name", t.name());
            o.addProperty("color", t.color());
            arr.add(o);
        }
        core.config().writeText(TEMPLATE_FILE, arr.toString());
    }

    // ---- Zeichnen ----

    @Override public void render(Renderer r, int mx, int my, float delta) {
        hits.clear();
        int w = r.screenWidth(), h = r.screenHeight();
        r.fill(0, 0, w, h, Theme.OVERLAY);
        int pw = Math.min(w - 20, 240), ph = 176;
        int tw = showTemplates ? Math.min(150, w - pw - 30) : 0;
        int px = (w - pw - (tw > 0 ? tw + 6 : 0)) / 2, py = (h - ph) / 2;
        Window.frame(r, px, py, px + pw, py + ph);
        Window.title(r, title(), px + 10, py + 9);
        button(r, mx, my, px + pw - 74, py + 6, px + pw - 8, py + 20, Lang.tr("aio.ui.waypoint_edit.templates", "Templates"), showTemplates, b -> showTemplates = !showTemplates);

        // Name
        float fy = py + 28;
        field(r, 0, px + 10, fy, pw - 20);
        // Koordinaten nebeneinander
        float cy = fy + 30;
        float cw = (pw - 20 - 12) / 3f;
        for (int i = 1; i <= 3; i++) field(r, i, px + 10 + (i - 1) * (cw + 6), cy, cw);
        button(r, mx, my, px + 10, cy + 28, px + 110, cy + 42, Lang.tr("aio.ui.waypoint_edit.current_position", "Current position"), false, b -> {
            var p = core.platform().playerPos();
            fields[1] = String.valueOf((int) Math.floor(p.x()));
            fields[2] = String.valueOf((int) Math.floor(p.y()));
            fields[3] = String.valueOf((int) Math.floor(p.z()));
        });

        // Farben
        float sy = cy + 52;
        r.text(Lang.tr("aio.ui.waypoint_edit.color", "Color"), px + 10, sy, Theme.TEXT_DIM, false);
        int[] pal = WaypointsModule.PALETTE;
        float sw = 14, gap = 4;
        for (int i = 0; i < pal.length; i++) {
            float x = px + 10 + i * (sw + gap), y = sy + 11;
            int c = pal[i];
            boolean sel = (color & 0xFFFFFF) == c;
            if (sel) r.outline(x - 2, y - 2, x + sw + 2, y + sw + 2, 1, Theme.TEXT);
            r.fill(x, y, x + sw, y + sw, 0xFF000000 | c);
            hits.add(new Hit(x - 1, y - 1, x + sw + 1, y + sw + 1, b -> color = c));
        }

        if (error != null) r.text(error, px + 10, py + ph - 34, Theme.DANGER, false);
        button(r, mx, my, px + pw - 128, py + ph - 22, px + pw - 70, py + ph - 8, Lang.tr("aio.ui.waypoint_edit.cancel", "Cancel"), false, b -> core.platform().closeScreen());
        button(r, mx, my, px + pw - 64, py + ph - 22, px + pw - 8, py + ph - 8, Lang.tr("aio.ui.waypoint_edit.save", "Save"), true, b -> save());

        if (tw > 0) renderTemplates(r, mx, my, px + pw + 6, py, tw, ph);
    }

    private void field(Renderer r, int i, float x, float y, float w) {
        r.text(LABELS[i], x, y, Theme.TEXT_DIM, false);
        float by = y + 10;
        boolean f = focus == i;
        r.fill(x, by, x + w, by + 14, f ? 0xFF2A2A34 : Theme.TRACK);
        if (f) r.fill(x, by + 13, x + w, by + 14, Theme.accent(core));
        String vis = r.trim(fields[i], w - 10);
        if (f && selectAll) TextEdit.drawSelection(r, x + 4, by + 3, vis);
        boolean caret = f && !selectAll && (System.currentTimeMillis() / 500) % 2 == 0;
        r.text(vis + (caret ? "_" : ""), x + 4, by + 3, Theme.TEXT, false);
        hits.add(new Hit(x, by, x + w, by + 14, b -> {
            focus = i;
            selectAll = false;
        }));
    }

    private void renderTemplates(Renderer r, int mx, int my, int x, int y, int w, int h) {
        Window.frame(r, x, y, x + w, y + h);
        Window.title(r, Lang.tr("aio.ui.waypoint_edit.templates", "Templates"), x + 8, y + 9);
        List<Template> list = templates();
        float ry = y + 24;
        int maxRows = (h - 50) / 15;
        for (int i = 0; i < Math.min(list.size(), maxRows); i++) {
            Template t = list.get(i);
            boolean hover = mx >= x + 4 && mx <= x + w - 4 && my >= ry && my <= ry + 13;
            Draw.round(r, x + 4, ry, x + w - 4, ry + 13, 2, hover ? Theme.CARD_HOVER : Theme.CARD);
            r.fill(x + 7, ry + 3, x + 14, ry + 10, 0xFF000000 | t.color());
            r.text(r.trim(t.name(), w - 40), x + 18, ry + 3, Theme.TEXT, false);
            // register ✖ first (hits are checked from the back, so here after the row)
            hits.add(new Hit(x + 4, ry, x + w - 18, ry + 13, b -> {
                fields[0] = t.name();
                color = t.color();
                focus = 0;
                selectAll = false;
            }));
            boolean hx = mx >= x + w - 16 && mx <= x + w - 4 && my >= ry && my <= ry + 13;
            r.text("✖", x + w - 13, ry + 3, hx ? Theme.DANGER : Theme.TEXT_MUTED, false);
            hits.add(new Hit(x + w - 16, ry, x + w - 4, ry + 13, b -> {
                templates().remove(t);
                saveTemplates();
            }));
            ry += 15;
        }
        button(r, mx, my, x + 4, y + h - 38, x + w - 4, y + h - 24, Lang.tr("aio.ui.waypoint_edit.save_current", "+ Save current"), false, b -> {
            String name = fields[0].trim();
            if (name.isEmpty()) return;
            templates().removeIf(t -> t.name().equalsIgnoreCase(name));
            templates().add(new Template(name, color & 0xFFFFFF));
            saveTemplates();
        });
        button(r, mx, my, x + 4, y + h - 20, x + w - 4, y + h - 6, Lang.tr("aio.ui.waypoint_edit.restore_defaults", "Restore defaults"), false, b -> {
            templates = new ArrayList<>(DEFAULT_TEMPLATES);
            saveTemplates();
        });
    }

    private void button(Renderer r, int mx, int my, float x1, float y1, float x2, float y2, String label, boolean accent, IntConsumer action) {
        boolean hover = mx >= x1 && mx <= x2 && my >= y1 && my <= y2;
        Draw.round(r, x1, y1, x2, y2, 2, accent ? Theme.accent(core) : hover ? 0xFF33333E : 0xFF24242C);
        r.centeredText(label, (x1 + x2) / 2, y1 + (y2 - y1 - 8) / 2, Theme.TEXT, false);
        hits.add(new Hit(x1, y1, x2, y2, action));
    }

    // ---- Speichern ----

    private void save() {
        String name = fields[0].trim();
        if (name.isEmpty()) {
            error = Lang.tr("aio.ui.waypoint_edit.please_enter_a_name", "Please enter a name");
            focus = 0;
            return;
        }
        int[] c = new int[3];
        for (int i = 0; i < 3; i++) {
            try {
                c[i] = Integer.parseInt(fields[i + 1].trim());
            } catch (NumberFormatException e) {
                error = LABELS[i + 1] + " must be a whole number";
                focus = i + 1;
                selectAll = true;
                return;
            }
        }
        if (original == null) {
            module.add(new Waypoint(name, c[0], c[1], c[2], core.platform().worldKey(), core.platform().dimension(),
                    color & 0xFFFFFF, true, false));
            core.platform().notify(Lang.tr("aio.ui.waypoint_edit.waypoint_added", "Waypoint \"%s\" added", name));
        } else {
            module.replace(original, new Waypoint(name, c[0], c[1], c[2], original.world(), original.dimension(),
                    color & 0xFFFFFF, original.visible(), original.death()));
        }
        core.platform().closeScreen();
    }

    // ---- Eingabe ----

    @Override public boolean mouseClicked(double mx, double my, int button) {
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (mx >= h.x1 && mx <= h.x2 && my >= h.y1 && my <= h.y2) {
                h.action.accept(button);
                return true;
            }
        }
        return false;
    }

    @Override public boolean keyPressed(String key) {
        if (Keys.ENTER.equals(key) || "key.keyboard.keypad.enter".equals(key)) {
            save();
            return true;
        }
        if ("key.keyboard.tab".equals(key)) {
            boolean shift = core.platform().isKeyDown(Keys.LEFT_SHIFT) || core.platform().isKeyDown(Keys.RIGHT_SHIFT);
            focus = Math.floorMod(focus + (shift ? -1 : 1), 4);
            selectAll = true;
            return true;
        }
        if (Keys.ESCAPE.equals(key)) return false; // closes
        TextEdit.Result res = TextEdit.key(core.platform(), fields[focus], selectAll, key);
        fields[focus] = clean(focus, res.text());
        selectAll = res.allSelected();
        if (res.handled()) error = null;
        return res.handled();
    }

    @Override public boolean charTyped(String text) {
        if (text == null || text.isEmpty() || text.codePointAt(0) < 32) return false;
        if (System.currentTimeMillis() - openedAt < 150) return true;
        if (focus > 0 && !text.matches("[-0-9]+")) return true; // coordinates: digits only
        fields[focus] = clean(focus, TextEdit.type(fields[focus], selectAll, text));
        selectAll = false;
        error = null;
        return true;
    }

    private static String clean(int field, String s) {
        int max = field == 0 ? 32 : 9;
        if (field > 0) s = s.replaceAll("[^-0-9]", "");
        return s.length() > max ? s.substring(0, max) : s;
    }
}
