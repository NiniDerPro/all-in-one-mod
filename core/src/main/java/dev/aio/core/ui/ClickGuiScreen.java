package dev.aio.core.ui;

import dev.aio.core.AioCore;
import dev.aio.core.config.ConfigManager;
import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.platform.Keys;
import dev.aio.core.platform.Renderer;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.ColorSetting;
import dev.aio.core.setting.EnumSetting;
import dev.aio.core.setting.KeybindSetting;
import dev.aio.core.setting.NumberSetting;
import dev.aio.core.setting.Setting;
import dev.aio.core.setting.StringSetting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

/**
 * Einstellungsmenü: Kategorien links, Module als Karten, pro Modul Ein/Aus, Einstellungen und
 * Tastenbelegung an einem Ort; unten links Profile (wechseln, neu, exportieren, importieren, löschen).
 *
 * <p>Immediate-Mode: Jeder Frame zeichnet alles neu und merkt sich die klickbaren Flächen.
 * Klicks werden gegen die Flächen des letzten Frames geprüft.
 */
public final class ClickGuiScreen implements UiScreen {
    private static final int SIDEBAR_W = 104;
    private static final int CARD_H = 44;
    private static final int CARD_MIN_W = 132;
    private static final int GAP = 6;
    private static final int ROW_H = 20;

    private record Hit(float x1, float y1, float x2, float y2, boolean clipped, IntConsumer action) {
        boolean contains(double x, double y) { return x >= x1 && x <= x2 && y >= y1 && y <= y2; }
    }

    private record SliderDrag(float x1, float x2, DoubleConsumer setter) {
        void apply(double mx) { setter.accept((mx - x1) / Math.max(1, x2 - x1)); }
    }

    private final AioCore core;
    /** null = "Alle". */
    private Category category = Category.HUD;
    private String search = "";
    private boolean searchFocused;
    private Module selected;
    private float scroll;
    private float maxScroll;

    private final List<Hit> hits = new ArrayList<>();
    private SliderDrag slider;
    private KeybindSetting listening;
    private StringSetting editing;
    private String editBuffer = "";
    private ColorSetting expandedColor;
    private boolean confirmDelete;
    private boolean deleteTouched;
    private String status;
    private int statusColor;
    private long statusUntil;

    // Inhaltsbereich des letzten Frames, für das Clipping der Hits
    private float viewY1, viewY2;
    private boolean clipHits;

    public ClickGuiScreen(AioCore core, Module focus) {
        this.core = core;
        if (focus != null) {
            category = focus.category();
            selected = focus;
        }
    }

    @Override public String title() { return AioCore.NAME; }

    // =====================================================================
    // Zeichnen
    // =====================================================================

    @Override public void render(Renderer r, int mx, int my, float delta) {
        hits.clear();
        int w = r.screenWidth(), h = r.screenHeight();
        int accent = Theme.accent(core);
        r.fill(0, 0, w, h, Theme.OVERLAY);

        int pw = Math.min(w - 20, 480);
        int ph = Math.min(h - 20, 290);
        int px = (w - pw) / 2;
        int py = (h - ph) / 2;
        r.fill(px, py, px + pw, py + ph, Theme.PANEL);
        r.fill(px, py, px + SIDEBAR_W, py + ph, Theme.SIDEBAR);
        Theme.accentBar(r, core, px, py, px + pw, py + 2);

        renderSidebar(r, mx, my, px, py, ph, accent);

        int cx = px + SIDEBAR_W + 8;
        int cw = pw - SIDEBAR_W - 16;
        int cy = py + 8;
        if (selected != null) renderSettingsHeader(r, mx, my, cx, cy, cw, accent);
        else renderListHeader(r, mx, my, cx, cy, cw, accent);

        int viewTop = cy + 20;
        int viewBottom = py + ph - 8;
        viewY1 = viewTop;
        viewY2 = viewBottom;
        r.enableScissor(cx - 2, viewTop, cx + cw + 2, viewBottom);
        clipHits = true;
        float contentH = selected != null
                ? renderSettings(r, mx, my, cx, viewTop - scroll, cw, accent)
                : renderCards(r, mx, my, cx, viewTop - scroll, cw, accent);
        clipHits = false;
        r.disableScissor();
        maxScroll = Math.max(0, contentH - (viewBottom - viewTop));
        scroll = Math.max(0, Math.min(maxScroll, scroll));
        if (maxScroll > 0) {
            float trackH = viewBottom - viewTop;
            float barH = Math.max(16, trackH * trackH / contentH);
            float barY = viewTop + (trackH - barH) * (scroll / maxScroll);
            r.fill(px + pw - 4, barY, px + pw - 2, barY + barH, 0x80FFFFFF);
        }

        if (status != null && System.currentTimeMillis() < statusUntil) {
            float sw = r.textWidth(status) + 12;
            float sx = px + SIDEBAR_W + (pw - SIDEBAR_W - sw) / 2f;
            float sy = py + ph - 20;
            r.fill(sx, sy, sx + sw, sy + 14, 0xF0202028);
            r.fill(sx, sy + 13, sx + sw, sy + 14, statusColor);
            r.text(status, sx + 6, sy + 3, Theme.TEXT, false);
        }
    }

    private void renderSidebar(Renderer r, int mx, int my, int px, int py, int ph, int accent) {
        dev.aio.core.hud.ColoredText.draw(r, AioCore.NAME, px + 10, py + 9,
                core.clientSettings().rainbowAccent.on() ? dev.aio.core.hud.ColorMode.CHROMA : dev.aio.core.hud.ColorMode.STATIC,
                accent, accent, 0.25f, true, 0);
        float y = py + 28;
        int used = 1; // "All"
        for (Category c : Category.values()) if (!core.modules().byCategory(c).isEmpty()) used++;
        // Zeilenhöhe an den Platz bis zum Profilbereich anpassen (kleine Bildschirme)
        float rowH = Math.max(11, Math.min(17, (ph - 62 - 28 - 26) / (float) Math.max(1, used)));
        y = sidebarRow(r, mx, my, px, y, rowH, null, "All", 0xFFFFFFFF);
        for (Category c : Category.values()) {
            if (core.modules().byCategory(c).isEmpty()) continue;
            y = sidebarRow(r, mx, my, px, y, rowH, c, c.displayName(), c.color());
        }

        y += 6;
        if (core.platform().inWorld()) {
            button(r, mx, my, px + 6, y, px + SIDEBAR_W - 6, y + 16, "Edit HUD", accent, true,
                    b -> core.openHudEditor());
        }

        // Profile unten
        ConfigManager cfg = core.config();
        float by = py + ph - 62;
        r.text("Profile", px + 8, by, Theme.TEXT_MUTED, false);
        by += 11;
        List<String> profiles = cfg.profiles();
        String active = cfg.activeProfile();
        button(r, mx, my, px + 6, by, px + 18, by + 14, "<", accent, false, b -> cycleProfile(-1));
        button(r, mx, my, px + SIDEBAR_W - 18, by, px + SIDEBAR_W - 6, by + 14, ">", accent, false, b -> cycleProfile(1));
        r.centeredText(r.trim(active, SIDEBAR_W - 44), px + SIDEBAR_W / 2f, by + 3, Theme.TEXT, false);
        if (profiles.size() > 1) {
            String count = (profiles.indexOf(active) + 1) + "/" + profiles.size();
            r.text(count, px + SIDEBAR_W - 8 - r.textWidth(count), by - 11, Theme.TEXT_MUTED, false);
        }
        by += 17;
        float half = (SIDEBAR_W - 12 - 3) / 2f;
        button(r, mx, my, px + 6, by, px + 6 + half, by + 14, "New", accent, false, b -> {
            String name = cfg.createProfile("profil");
            showStatus("Profile \"" + name + "\" created", accent);
        });
        button(r, mx, my, px + 9 + half, by, px + SIDEBAR_W - 6, by + 14, confirmDelete ? "Sure?" : "Delete",
                confirmDelete ? Theme.DANGER : accent, false, b -> deleteActiveProfile());
        by += 17;
        button(r, mx, my, px + 6, by, px + 6 + half, by + 14, "Export", accent, false, b -> {
            core.platform().setClipboard(cfg.exportProfile());
            showStatus("Profile copied to clipboard and saved to exports/", accent);
        });
        button(r, mx, my, px + 9 + half, by, px + SIDEBAR_W - 6, by + 14, "Import", accent, false, b -> {
            try {
                String name = cfg.importProfile(core.platform().clipboard());
                showStatus("Profile \"" + name + "\" imported", accent);
            } catch (IllegalArgumentException e) {
                showStatus("Import failed: " + e.getMessage(), Theme.DANGER);
            }
        });
    }

    private float sidebarRow(Renderer r, int mx, int my, int px, float y, float rowH, Category c, String label, int cc) {
        boolean sel = c == category && search.isEmpty();
        boolean hover = inside(mx, my, px, y, px + SIDEBAR_W, y + rowH - 1);
        float rh = rowH - 1, ty = y + (rh - 8) / 2f;
        if (sel) {
            r.fill(px, y, px + SIDEBAR_W, y + rh, dev.aio.core.util.Colors.withAlpha(cc, 0x38));
            r.fill(px, y, px + 2, y + rh, cc);
        } else if (hover) {
            r.fill(px, y, px + SIDEBAR_W, y + rh, dev.aio.core.util.Colors.withAlpha(cc, 0x18));
        }
        r.fill(px + 10, ty + 2, px + 14, ty + 6, cc);
        r.text(r.trim(label, SIDEBAR_W - 24), px + 19, ty, sel ? Theme.TEXT : Theme.TEXT_DIM, false);
        hit(px, y, px + SIDEBAR_W, y + rh, b -> {
            category = c;
            selected = null;
            scroll = 0;
            search = "";
            resetFocus();
        });
        return y + rowH;
    }

    /** Module der Ansicht: Suche (über alle Kategorien), sonst Kategorie bzw. "Alle"; dann sortiert. */
    private List<Module> visibleModules() {
        List<Module> base = !search.isEmpty() || category == null ? core.modules().all() : core.modules().byCategory(category);
        return core.clientSettings().sortMode.get().sort(SortMode.search(base, search));
    }

    private void renderListHeader(Renderer r, int mx, int my, float x, float y, float w, int accent) {
        String title = !search.isEmpty() ? "Search" : category == null ? "All modules" : category.displayName();
        int color = category == null || !search.isEmpty() ? Theme.TEXT : category.color();
        r.text(title, x, y + 3, color, true);
        r.fill(x, y + 14, x + r.textWidth(title), y + 15, color);

        // Suchfeld rechts
        float sw = Math.min(110, w * 0.38f);
        float sx2 = x + w, sx1 = sx2 - sw;
        r.fill(sx1, y, sx2, y + 14, searchFocused ? 0xFF2A2A34 : Theme.TRACK);
        if (searchFocused) r.fill(sx1, y + 13, sx2, y + 14, accent);
        String shown = search.isEmpty() && !searchFocused ? "Search…" : search;
        while (r.textWidth(shown) > sw - 10 && shown.length() > 1) shown = shown.substring(1);
        boolean caret = searchFocused && (System.currentTimeMillis() / 500) % 2 == 0;
        r.text(shown + (caret ? "_" : ""), sx1 + 4, y + 3, search.isEmpty() && !searchFocused ? Theme.TEXT_MUTED : Theme.TEXT, false);
        hit(sx1, y, sx2, y + 14, b -> {
            resetFocus();
            searchFocused = true;
            if (b == 1) search = "";
        });

        // Sortier-Knopf links daneben
        String label = "↕ " + core.clientSettings().sortMode.get().displayName();
        float bw = r.textWidth(label) + 10;
        float bx2 = sx1 - 4, bx1 = bx2 - bw;
        float titleEnd = x + r.textWidth(title) + 6;
        if (bx1 > titleEnd) {
            button(r, mx, my, bx1, y, bx2, y + 14, label, accent, false,
                    b -> core.clientSettings().sortMode.cycle(b == 1 ? -1 : 1));
        } else {
            bx1 = sx1;
        }
        // Alle an / Alle aus: gilt für die gerade gezeigten Module (Kategorie bzw. Suchtreffer)
        float offW = r.textWidth("All off") + 10, onW = r.textWidth("All on") + 10;
        float ox2 = bx1 - 4, ox1 = ox2 - offW, nx2 = ox1 - 3, nx1 = nx2 - onW;
        if (nx1 > titleEnd) {
            button(r, mx, my, nx1, y, nx2, y + 14, "All on", accent, false, b -> setAll(true));
            button(r, mx, my, ox1, y, ox2, y + 14, "All off", accent, false, b -> setAll(false));
        }
    }

    private void setAll(boolean on) {
        for (Module m : visibleModules()) if (m.canToggle()) m.setEnabled(on);
    }

    private float renderCards(Renderer r, int mx, int my, float x, float y, float w, int accent) {
        List<Module> list = visibleModules();
        int cols = Math.max(1, (int) ((w + GAP) / (CARD_MIN_W + GAP)));
        float cardW = (w - (cols - 1) * GAP) / cols;
        for (int i = 0; i < list.size(); i++) {
            Module m = list.get(i);
            float x1 = x + (i % cols) * (cardW + GAP);
            float y1 = y + (i / cols) * (CARD_H + GAP);
            float x2 = x1 + cardW, y2 = y1 + CARD_H;
            boolean hover = inside(mx, my, x1, y1, x2, y2) && my >= viewY1 && my <= viewY2;
            int cc = m.category().color();
            r.fill(x1, y1, x2, y2, hover ? Theme.CARD_HOVER : Theme.CARD);
            if (m.isActive()) {
                r.fill(x1, y1, x2, y2, dev.aio.core.util.Colors.withAlpha(cc, hover ? 0x2A : 0x1C));
                r.fill(x1, y1, x1 + 2, y2, cc);
            } else if (m.isEnabled() && m.isBlocked()) {
                r.fill(x1, y1, x1 + 2, y2, Theme.WARNING);
            }

            r.text(r.trim(m.name(), cardW - 40), x1 + 8, y1 + 7, Theme.TEXT, false);
            r.text(r.trim(m.description(), cardW - 16), x1 + 8, y1 + 19, Theme.TEXT_DIM, false);
            String footer = m.isBlocked() ? "Blocked (server)" :
                    m.toggleKey().isBound() ? "Key: " + core.platform().keyName(m.toggleKey().key()) :
                    category == null || !search.isEmpty() ? m.category().displayName() : "";
            r.text(r.trim(footer, cardW - 30), x1 + 8, y1 + 31, m.isBlocked() ? Theme.WARNING : Theme.TEXT_MUTED, false);
            r.text("›", x2 - 10, y1 + 31, hover ? Theme.TEXT : Theme.TEXT_MUTED, false);

            // Karte öffnet die Einstellungen, der Schalter schaltet um (Schalter wird später registriert und gewinnt)
            hit(x1, y1, x2, y2, b -> openSettings(m));
            if (m.canToggle()) {
                float sx = x2 - 28, sy = y1 + 6;
                toggleSwitch(r, sx, sy, m.isEnabled(), m.isBlocked(), cc);
                hit(sx - 2, sy - 2, sx + 22, sy + 12, b -> m.toggle());
            }
        }
        int rows = (list.size() + cols - 1) / cols;
        return rows * (CARD_H + GAP) - GAP;
    }

    private void renderSettingsHeader(Renderer r, int mx, int my, float x, float y, float w, int accent) {
        button(r, mx, my, x, y, x + 54, y + 14, "‹ Back", accent, false, b -> closeSettings());
        r.text(r.trim(selected.name(), w - 120), x + 62, y + 3, Theme.TEXT, false);
        if (selected.canToggle()) {
            float sx = x + w - 22, sy = y + 2;
            toggleSwitch(r, sx, sy, selected.isEnabled(), selected.isBlocked(), accent);
            Module m = selected;
            hit(sx - 2, sy - 2, sx + 22, sy + 12, b -> m.toggle());
            if (m.isBlocked()) {
                String t = "Blocked";
                r.text(t, sx - 6 - r.textWidth(t), y + 3, Theme.WARNING, false);
            }
        }
    }

    private float renderSettings(Renderer r, int mx, int my, float x, float y, float w, int accent) {
        float startY = y;
        r.text(r.trim(selected.description(), w), x, y, Theme.TEXT_DIM, false);
        y += 14;
        if (selected.isGameplayAffecting()) {
            r.text(r.trim("May count as an advantage on servers and is blocked by compatibility mode.", w), x, y, Theme.WARNING, false);
            y += 14;
        }
        y += 2;
        for (Setting<?> s : selected.settings()) {
            if (!s.isVisible()) continue;
            y = renderSetting(r, mx, my, s, x, y, w, accent);
        }
        y += 6;
        Module m = selected;
        button(r, mx, my, x, y, x + 90, y + 14, "Reset", Theme.DANGER, false, b -> {
            // Versteckte Einstellungen (Position, gespeicherte Daten) bleiben unangetastet
            for (Setting<?> s : m.settings()) {
                if (!s.isHidden()) s.reset();
            }
            showStatus(m.name() + " reset", accent);
        });
        y += 14;
        return y - startY;
    }

    private float renderSetting(Renderer r, int mx, int my, Setting<?> s, float x, float y, float w, int accent) {
        float right = x + w;
        float textY = y + (ROW_H - 8) / 2f;
        r.fill(x, y, right, y + ROW_H, 0x10FFFFFF);
        r.text(r.trim(s.name(), w * 0.5f), x + 6, textY, Theme.TEXT, false);

        if (s instanceof dev.aio.core.setting.ActionSetting a) {
            float bw = Math.max(70, r.textWidth(a.name()) + 16);
            button(r, mx, my, right - 4 - bw, y + 3, right - 4, y + ROW_H - 3, "Open", accent, true, btn -> a.run());
            return y + ROW_H + 2;
        }
        if (s instanceof BooleanSetting b) {
            float sx = right - 26, sy = y + 5;
            toggleSwitch(r, sx, sy, b.on(), false, accent);
            hit(x, y, right, y + ROW_H, btn -> b.toggle());
            return y + ROW_H + 2;
        }
        if (s instanceof NumberSetting n) {
            String v = n.display();
            r.text(v, right - 6 - r.textWidth(v), textY, Theme.TEXT_DIM, false);
            float tx1 = x + w * 0.5f, tx2 = right - 12 - r.textWidth(v);
            slider(r, mx, my, tx1, tx2, y + ROW_H / 2f, n.fraction(), accent, n::setFraction);
            return y + ROW_H + 2;
        }
        if (s instanceof EnumSetting<?> e) {
            String v = "< " + e.display() + " >";
            float bx = right - 6 - r.textWidth(v);
            boolean hover = inside(mx, my, bx - 4, y, right, y + ROW_H);
            r.text(v, bx, textY, hover ? Theme.TEXT : Theme.TEXT_DIM, false);
            hit(x, y, right, y + ROW_H, btn -> e.cycle(btn == 1 ? -1 : 1));
            return y + ROW_H + 2;
        }
        if (s instanceof KeybindSetting k) {
            String v = listening == k ? "> Press a key <" : (k.isBound() ? core.platform().keyName(k.key()) : "None");
            float bw = Math.max(50, r.textWidth(v) + 12);
            float bx = right - 4 - bw;
            boolean hover = inside(mx, my, bx, y + 3, right - 4, y + ROW_H - 3);
            r.fill(bx, y + 3, right - 4, y + ROW_H - 3, listening == k ? accent : hover ? 0xFF3A3A46 : Theme.TRACK);
            r.centeredText(v, bx + bw / 2f, textY, Theme.TEXT, false);
            hit(bx, y + 3, right - 4, y + ROW_H - 3, btn -> {
                if (listening == k && btn >= 2) { // Mitte und Seitentasten lassen sich als Belegung nutzen
                    k.set(Keys.mouse(btn));
                    listening = null;
                } else {
                    resetFocus();
                    listening = k;
                }
            });
            return y + ROW_H + 2;
        }
        if (s instanceof StringSetting str) {
            boolean focused = editing == str;
            float fx = x + w * 0.45f;
            r.fill(fx, y + 3, right - 4, y + ROW_H - 3, focused ? 0xFF2A2A34 : Theme.TRACK);
            if (focused) r.fill(fx, y + ROW_H - 4, right - 4, y + ROW_H - 3, accent);
            String shown = focused ? editBuffer : str.get();
            if (shown.isEmpty() && !focused) shown = "—";
            String vis = shown;
            float maxW = right - fx - 12;
            while (r.textWidth(vis) > maxW && vis.length() > 1) vis = vis.substring(1);
            boolean caret = focused && (System.currentTimeMillis() / 500) % 2 == 0;
            r.text(vis + (caret ? "_" : ""), fx + 4, textY, focused ? Theme.TEXT : Theme.TEXT_DIM, false);
            hit(fx, y + 3, right - 4, y + ROW_H - 3, btn -> {
                if (editing != str) {
                    resetFocus();
                    editing = str;
                    editBuffer = str.get();
                }
            });
            return y + ROW_H + 2;
        }
        if (s instanceof ColorSetting c) {
            float sw = 24;
            float sx = right - 6 - sw;
            // Schachbrett für Transparenz
            r.fill(sx, y + 4, sx + sw, y + ROW_H - 4, 0xFFBBBBBB);
            r.fill(sx, y + 4, sx + sw / 2, y + ROW_H / 2f, 0xFF777777);
            r.fill(sx + sw / 2, y + ROW_H / 2f, sx + sw, y + ROW_H - 4, 0xFF777777);
            r.fill(sx, y + 4, sx + sw, y + ROW_H - 4, c.argb());
            r.outline(sx, y + 4, sx + sw, y + ROW_H - 4, 1, 0x80FFFFFF);
            String hex = String.format("#%08X", c.argb());
            r.text(hex, sx - 6 - r.textWidth(hex), textY, Theme.TEXT_MUTED, false);
            hit(x, y, right, y + ROW_H, btn -> expandedColor = expandedColor == c ? null : c);
            y += ROW_H;
            if (expandedColor == c) {
                String[] labels = {"Red", "Green", "Blue", "Opacity"};
                int[] shifts = {16, 8, 0, 24};
                for (int i = 0; i < 4; i++) {
                    int shift = shifts[i];
                    r.fill(x, y, right, y + 16, 0x08FFFFFF);
                    r.text(labels[i], x + 14, y + 4, Theme.TEXT_DIM, false);
                    String v = String.valueOf(c.channel(shift));
                    r.text(v, right - 6 - r.textWidth(v), y + 4, Theme.TEXT_MUTED, false);
                    slider(r, mx, my, x + w * 0.5f, right - 30, y + 8, c.channel(shift) / 255.0, accent,
                            f -> c.setChannel(shift, (int) Math.round(f * 255)));
                    y += 16;
                }
            }
            return y + 2;
        }
        return y + ROW_H + 2;
    }

    // =====================================================================
    // Widgets
    // =====================================================================

    private void toggleSwitch(Renderer r, float x, float y, boolean on, boolean blocked, int accent) {
        int track = on ? (blocked ? Theme.WARNING : accent) : Theme.TRACK;
        r.fill(x, y, x + 20, y + 10, track);
        float kx = on ? x + 11 : x + 1;
        r.fill(kx, y + 1, kx + 8, y + 9, 0xFFFFFFFF);
    }

    private void slider(Renderer r, int mx, int my, float x1, float x2, float cy, double fraction, int accent, DoubleConsumer setter) {
        if (x2 - x1 < 10) return;
        r.fill(x1, cy - 1, x2, cy + 1, Theme.TRACK);
        float fx = (float) (x1 + (x2 - x1) * Math.max(0, Math.min(1, fraction)));
        r.fill(x1, cy - 1, fx, cy + 1, accent);
        boolean hover = inside(mx, my, x1 - 3, cy - 6, x2 + 3, cy + 6);
        r.fill(fx - 2, cy - 4, fx + 2, cy + 4, hover || slider != null ? 0xFFFFFFFF : 0xFFDDDDDD);
        hit(x1 - 3, cy - 7, x2 + 3, cy + 7, b -> {
            if (b != 0) return;
            slider = new SliderDrag(x1, x2, setter);
        });
    }

    private void button(Renderer r, int mx, int my, float x1, float y1, float x2, float y2, String label,
                        int accent, boolean filled, IntConsumer action) {
        boolean hover = inside(mx, my, x1, y1, x2, y2);
        int bg = filled ? (hover ? brighten(accent) : accent) : (hover ? 0xFF33333E : 0xFF24242C);
        r.fill(x1, y1, x2, y2, bg);
        if (!filled && hover) r.fill(x1, y2 - 1, x2, y2, accent);
        r.centeredText(r.trim(label, x2 - x1 - 4), (x1 + x2) / 2f, y1 + (y2 - y1 - 8) / 2f, Theme.TEXT, false);
        hit(x1, y1, x2, y2, action);
    }

    private static int brighten(int c) {
        int r = Math.min(255, ((c >> 16) & 0xFF) + 25);
        int g = Math.min(255, ((c >> 8) & 0xFF) + 25);
        int b = Math.min(255, (c & 0xFF) + 25);
        return (c & 0xFF000000) | r << 16 | g << 8 | b;
    }

    /** Registriert eine klickbare Fläche; im Inhaltsbereich wird sie auf den sichtbaren Teil beschnitten. */
    private void hit(float x1, float y1, float x2, float y2, IntConsumer action) {
        hits.add(new Hit(x1, y1, x2, y2, clipHits, action));
    }

    private static boolean inside(double mx, double my, float x1, float y1, float x2, float y2) {
        return mx >= x1 && mx <= x2 && my >= y1 && my <= y2;
    }

    // =====================================================================
    // Aktionen
    // =====================================================================

    private void openSettings(Module m) {
        selected = m;
        scroll = 0;
        resetFocus();
    }

    private void closeSettings() {
        selected = null;
        scroll = 0;
        resetFocus();
    }

    private void resetFocus() {
        searchFocused = false;
        commitEdit();
        listening = null;
        expandedColor = null;
        confirmDelete = false;
    }

    private void commitEdit() {
        if (editing != null) {
            editing.set(editBuffer);
            editing = null;
        }
    }

    private void cycleProfile(int dir) {
        List<String> profiles = core.config().profiles();
        if (profiles.size() < 2) return;
        int idx = profiles.indexOf(core.config().activeProfile());
        String next = profiles.get(Math.floorMod(idx + dir, profiles.size()));
        core.config().switchProfile(next);
        showStatus("Profile: " + next, Theme.accent(core));
    }

    private void deleteActiveProfile() {
        deleteTouched = true;
        ConfigManager cfg = core.config();
        String active = cfg.activeProfile();
        List<String> profiles = cfg.profiles();
        if (profiles.size() < 2) {
            showStatus("The last profile cannot be deleted", Theme.DANGER);
            return;
        }
        if (!confirmDelete) {
            confirmDelete = true;
            return;
        }
        confirmDelete = false;
        String fallback = profiles.contains(ConfigManager.DEFAULT_PROFILE) && !active.equals(ConfigManager.DEFAULT_PROFILE)
                ? ConfigManager.DEFAULT_PROFILE
                : profiles.get(profiles.indexOf(active) == 0 ? 1 : 0);
        cfg.switchProfile(fallback);
        if (cfg.deleteProfile(active)) showStatus("Profile \"" + active + "\" deleted", Theme.accent(core));
    }

    private void showStatus(String message, int color) {
        status = message;
        statusColor = color;
        statusUntil = System.currentTimeMillis() + 3000;
    }

    // =====================================================================
    // Eingaben
    // =====================================================================

    @Override public boolean mouseClicked(double mx, double my, int button) {
        boolean wasConfirming = confirmDelete;
        deleteTouched = false;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (!h.contains(mx, my) || !isClickable(h, my)) continue;
            commitEdit(); // Textfeld verliert den Fokus, ein Klick auf das Feld fokussiert es neu
            h.action.accept(button);
            if (slider != null) slider.apply(mx);
            if (wasConfirming && !deleteTouched) confirmDelete = false;
            return true;
        }
        commitEdit();
        listening = null;
        confirmDelete = false;
        return false;
    }

    /** Hits im scrollbaren Inhaltsbereich zählen nur im sichtbaren Teil. */
    private boolean isClickable(Hit h, double my) {
        return !h.clipped || (my >= viewY1 && my <= viewY2);
    }

    @Override public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (slider != null) {
            slider.apply(mx);
            return true;
        }
        return false;
    }

    @Override public boolean mouseReleased(double mx, double my, int button) {
        if (slider != null) {
            slider = null;
            return true;
        }
        return false;
    }

    @Override public boolean mouseScrolled(double mx, double my, double amount) {
        scroll = (float) Math.max(0, Math.min(maxScroll, scroll - amount * 20));
        return true;
    }

    @Override public boolean keyPressed(String key) {
        if (searchFocused) {
            boolean ctrl = core.platform().isKeyDown(Keys.LEFT_CONTROL) || core.platform().isKeyDown(Keys.RIGHT_CONTROL);
            if (Keys.ESCAPE.equals(key) || Keys.ENTER.equals(key)) searchFocused = false;
            else if (Keys.BACKSPACE.equals(key) && !search.isEmpty()) setSearch(search.substring(0, search.length() - 1));
            else if (ctrl && Keys.V.equals(key)) setSearch(search + core.platform().clipboard().replaceAll("[\\r\\n]", ""));
            return true;
        }
        if (selected == null && "key.keyboard.f".equals(key)
                && (core.platform().isKeyDown(Keys.LEFT_CONTROL) || core.platform().isKeyDown(Keys.RIGHT_CONTROL))) {
            searchFocused = true;
            return true;
        }
        if (listening != null) {
            listening.set(Keys.ESCAPE.equals(key) || Keys.BACKSPACE.equals(key) || Keys.DELETE.equals(key) ? Keys.NONE : key);
            listening = null;
            return true;
        }
        if (editing != null) {
            boolean ctrl = core.platform().isKeyDown(Keys.LEFT_CONTROL) || core.platform().isKeyDown(Keys.RIGHT_CONTROL);
            if (Keys.ESCAPE.equals(key)) {
                editing = null;
            } else if (Keys.ENTER.equals(key)) {
                commitEdit();
            } else if (Keys.BACKSPACE.equals(key)) {
                if (!editBuffer.isEmpty()) editBuffer = editBuffer.substring(0, editBuffer.offsetByCodePoints(editBuffer.length(), -1));
            } else if (ctrl && Keys.V.equals(key)) {
                insert(core.platform().clipboard());
            } else if (ctrl && Keys.C.equals(key)) {
                core.platform().setClipboard(editBuffer);
            }
            return true;
        }
        if (Keys.ESCAPE.equals(key) && selected != null) {
            closeSettings();
            return true;
        }
        if (core.clientSettings().guiKey.matches(key)) {
            core.platform().closeScreen();
            return true;
        }
        return false;
    }

    private void setSearch(String q) {
        search = q.length() > 40 ? q.substring(0, 40) : q;
        scroll = 0;
    }

    @Override public boolean charTyped(String text) {
        // Einfach lostippen: ohne anderes aktives Feld landet der Text in der Suche oben rechts
        if (!searchFocused && editing == null && listening == null && selected == null
                && text != null && !text.isEmpty() && text.codePointAt(0) > 32) {
            searchFocused = true;
        }
        if (searchFocused && text != null && !text.isEmpty() && text.codePointAt(0) >= 32) {
            setSearch(search + text);
            return true;
        }
        if (editing == null || text == null || text.isEmpty() || text.codePointAt(0) < 32 || text.codePointAt(0) == 127) return false;
        insert(text);
        return true;
    }

    private void insert(String text) {
        if (text == null || editing == null) return;
        String clean = text.replaceAll("[\\r\\n\\t]", "");
        editBuffer = (editBuffer + clean);
        if (editBuffer.length() > editing.maxLength()) editBuffer = editBuffer.substring(0, editing.maxLength());
    }

    /** Aus dem HUD-Editor (Rechtsklick) geöffnet: ESC führt zurück in den Editor. */
    @Override public boolean returnsToPrevious() { return true; }

    @Override public void onClose() {
        commitEdit();
        core.config().save();
    }
}
