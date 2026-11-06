package dev.aio.core.ui;

import dev.aio.core.AioCore;
import dev.aio.core.config.ConfigManager;
import dev.aio.core.i18n.Lang;
import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.platform.Keys;
import dev.aio.core.platform.Renderer;
import dev.aio.core.setting.ActionSetting;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.ColorSetting;
import dev.aio.core.setting.EnumSetting;
import dev.aio.core.setting.KeybindSetting;
import dev.aio.core.setting.NumberSetting;
import dev.aio.core.setting.Setting;
import dev.aio.core.setting.StringSetting;
import dev.aio.core.util.Colors;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

/**
 * Hauptmenü. Oben eine Leiste mit Kategorien als Reitern, Suche, HUD-Editor und Zahnrad für die
 * Client-Einstellungen; darunter die Module als Liste. Ein Klick auf ein Modul schiebt rechts ein
 * Panel mit seinen Einstellungen herein, die Liste bleibt daneben sichtbar. Unten Profil und Meldungen.
 *
 * <p>Immediate-Mode: jeder Frame zeichnet alles neu und merkt sich die klickbaren Flächen.
 */
public final class ClickGuiScreen implements UiScreen {
    private static final int TOP = 26, BOTTOM = 18, ROW_H = 28, GAP = 4, SET_H = 18;
    /** Einstellungen, die bei HUD-Elementen unter "Appearance" gruppiert werden. */
    private static final Set<String> APPEARANCE = Set.of("scale", "scaleY", "colorMode", "textColor", "secondColor",
            "colorSpeed", "background", "backgroundColor", "shadow");

    private record Hit(float x1, float y1, float x2, float y2, int clip, IntConsumer action) {
        boolean contains(double x, double y) { return x >= x1 && x <= x2 && y >= y1 && y <= y2; }
    }

    private record SliderDrag(float x1, float x2, DoubleConsumer setter) {
        void apply(double mx) { setter.accept((mx - x1) / Math.max(1, x2 - x1)); }
    }

    /** Was im Panel rechts gezeigt wird: ein Modul oder die Client-Einstellungen. */
    private Module drawer;
    private boolean clientDrawer;

    private final AioCore core;
    private final Anim anim = new Anim();
    /** null = alle Kategorien. */
    private Category category;
    private String search = "";
    private boolean searchFocused, searchAll, editAll;
    private float listScroll, listMax, drawerScroll, drawerMax;

    private final List<Hit> hits = new ArrayList<>();
    /** Sichtbare Bereiche der beiden scrollbaren Flächen (für das Beschneiden der Klickflächen). */
    private float listY1, listY2, drawerX1, drawerY1, drawerY2;
    private int clip;
    private SliderDrag slider;
    private KeybindSetting listening;
    /** Das Zeichen zur gerade belegten Taste kommt danach noch als charTyped und darf nicht in die Suche. */
    private boolean swallowChar;
    private StringSetting editing;
    private String editBuffer = "";
    private ColorSetting expandedColor;
    private boolean appearanceOpen;
    private boolean confirmDelete, deleteTouched;
    private long resetAllArmed;
    private String status, hint;
    private int statusColor;
    private long statusUntil;

    /** Stand beim letzten Schließen, damit das Menü dort wieder aufgeht (nur für diese Sitzung). */
    private record Memory(AioCore owner, Category category, String search, float listScroll, String drawerId, boolean clientDrawer,
                          float drawerScroll, boolean appearanceOpen) {}
    private static Memory memory;

    public ClickGuiScreen(AioCore core, Module focus) {
        this.core = core;
        if (focus == null && memory != null && memory.owner() == core && core.clientSettings().rememberMenu.on()) {
            restore(memory);
        } else if (focus == core.clientSettings()) {
            clientDrawer = true;
            anim.set("drawer", 1);
        } else if (focus != null) {
            if (focus.category() != Category.CLIENT) category = focus.category();
            openDrawer(focus);
            anim.set("drawer", 1);
        }
    }

    @Override public String title() { return AioCore.NAME; }

    // =====================================================================
    // Aufbau
    // =====================================================================

    @Override public void render(Renderer r, int mx, int my, float delta) {
        anim.frame();
        hits.clear();
        hint = null;
        int w = r.screenWidth(), h = r.screenHeight();
        int accent = Theme.accent(core);
        r.fill(0, 0, w, h, Theme.OVERLAY);

        int pw = Math.min(w - 24, 540), ph = Math.min(h - 24, 310);
        int px = (w - pw) / 2, py = (h - ph) / 2;
        // Schatten, Fläche, feiner Rand
        Draw.round(r, px - 2, py - 1, px + pw + 2, py + ph + 3, 3, 0x40000000);
        Draw.round(r, px, py, px + pw, py + ph, 3, Theme.PANEL);
        Draw.roundOutline(r, px, py, px + pw, py + ph, Theme.LINE);

        renderTopBar(r, mx, my, px, py, pw, accent);
        int bodyTop = py + TOP, bodyBottom = py + ph - BOTTOM;
        renderList(r, mx, my, px + 8, bodyTop, pw - 16, bodyBottom, accent);
        renderDrawer(r, mx, my, px, pw, bodyTop, bodyBottom, accent);
        renderBottomBar(r, mx, my, px, py + ph - BOTTOM, pw, accent);
    }

    // ---- Obere Leiste ----

    private void renderTopBar(Renderer r, int mx, int my, int px, int py, int pw, int accent) {
        r.fill(px + 1, py + TOP - 1, px + pw - 1, py + TOP, Theme.LINE);
        // Schriftzug
        float x = px + 10, ty = py + 9;
        Draw.round(r, x, ty - 1, x + 3, ty + 8, 1, accent);
        Draw.bold(r, "AIO", x + 6, ty, Theme.TEXT);
        x += 6 + Draw.boldWidth(r, "AIO") + 14;

        // Rechts: Zahnrad, HUD-Editor, Suche
        float rx = px + pw - 8;
        rx = iconButton(r, mx, my, rx - 16, py + 5, Draw.GEAR, clientDrawer && drawerOpen(),
                Lang.tr("aio.ui.menu.client_settings", "Client settings"), accent, b -> toggleClientDrawer()) - 4;
        if (core.platform().inWorld()) {
            rx = iconButton(r, mx, my, rx - 16, py + 5, Draw.LAYOUT, false,
                    Lang.tr("aio.ui.menu.edit_hud", "Edit HUD layout"), accent, b -> core.openHudEditor()) - 6;
        }

        // Reiter
        List<Category> tabs = tabs();
        float tabsEnd = x;
        float[] tabX = new float[tabs.size() + 1];
        float[] tabW = new float[tabs.size() + 1];
        for (int i = 0; i <= tabs.size(); i++) {
            String label = i == 0 ? Lang.tr("aio.ui.menu.all", "All") : tabs.get(i - 1).displayName();
            tabX[i] = tabsEnd;
            tabW[i] = r.textWidth(label);
            tabsEnd += tabW[i] + 12;
        }
        float searchW = Math.max(54, Math.min(120, rx - tabsEnd - 6));
        // Beim Suchen wächst das Feld über die Reiter (die dann ohnehin nicht gelten)
        boolean typing = searchFocused || !search.isEmpty();
        float wanted = typing ? Math.min(rx - x - 4, Math.max(searchW, Math.max(160, r.textWidth(search) + 30))) : searchW;
        searchW = anim.to("searchW", wanted, 14);
        float searchX = rx - searchW;
        renderSearch(r, mx, my, searchX, py + 5, rx, accent);

        int selIndex = category == null ? 0 : tabs.indexOf(category) + 1;
        boolean searching = !search.isEmpty();
        for (int i = 0; i <= tabs.size(); i++) {
            if (tabX[i] + tabW[i] + 4 > searchX) break; // vom Suchfeld überdeckt
            Category c = i == 0 ? null : tabs.get(i - 1);
            String label = i == 0 ? Lang.tr("aio.ui.menu.all", "All") : c.displayName();
            boolean sel = i == selIndex && !searching;
            boolean hover = inside(mx, my, tabX[i] - 4, py + 4, tabX[i] + tabW[i] + 4, py + TOP - 2);
            float lit = anim.to("tab" + i, sel ? 1 : hover ? 0.6f : 0, 18);
            r.text(label, tabX[i], py + 10, Colors.lerp(Theme.TEXT_DIM, Theme.TEXT, lit), false);
            hit(tabX[i] - 4, py + 4, tabX[i] + tabW[i] + 4, py + TOP - 2, b -> selectCategory(c));
        }
        // Markierung unter dem gewählten Reiter gleitet mit
        if (!searching && tabX[selIndex] + tabW[selIndex] + 4 <= searchX) {
            float ux = anim.to("tabX", tabX[selIndex], 16), uw = anim.to("tabW", tabW[selIndex], 16);
            Draw.round(r, ux, py + TOP - 3, ux + uw, py + TOP - 1, 1, accent);
        }
    }

    private List<Category> tabs() {
        List<Category> out = new ArrayList<>();
        for (Category c : Category.values()) {
            if (c != Category.CLIENT && !core.modules().byCategory(c).isEmpty()) out.add(c);
        }
        return out;
    }

    private float iconButton(Renderer r, int mx, int my, float x, float y, String[] icon, boolean active, String tip,
                             int accent, IntConsumer action) {
        boolean hover = inside(mx, my, x, y, x + 16, y + 16);
        float t = anim.to(icon, active ? 1 : hover ? 0.5f : 0, 18);
        Draw.round(r, x, y, x + 16, y + 16, 2, Colors.lerp(0x00000000, active ? Colors.withAlpha(accent, 0x40) : Theme.CARD_HOVER, Math.min(1, t * 2)));
        Draw.icon(r, icon, x + 4, y + 4, active ? accent : Colors.lerp(Theme.TEXT_DIM, Theme.TEXT, t));
        if (hover) hint = tip;
        hit(x, y, x + 16, y + 16, action);
        return x;
    }

    private void renderSearch(Renderer r, int mx, int my, float x1, float y, float x2, int accent) {
        boolean hover = inside(mx, my, x1, y, x2, y + 16);
        Draw.round(r, x1, y, x2, y + 16, 2, searchFocused ? 0xFF1E2230 : hover ? 0xFF1A1E28 : 0xFF151821);
        if (searchFocused) Draw.roundOutline(r, x1, y, x2, y + 16, Colors.withAlpha(accent, 0xC0));
        Draw.icon(r, Draw.SEARCH, x1 + 4, y + 4, searchFocused ? accent : Theme.TEXT_MUTED);
        float tx = x1 + 15, maxW = x2 - tx - 4;
        if (search.isEmpty() && !searchFocused) {
            r.text(r.trim(Lang.tr("aio.ui.menu.search", "Search"), maxW), tx, y + 4, Theme.TEXT_MUTED, false);
        } else {
            String shown = search;
            while (r.textWidth(shown) > maxW - 4 && shown.length() > 1) shown = shown.substring(1);
            if (searchFocused && searchAll) TextEdit.drawSelection(r, tx, y + 4, shown);
            r.text(shown, tx, y + 4, Theme.TEXT, false);
            if (searchFocused && !searchAll && (System.currentTimeMillis() / 500) % 2 == 0) {
                r.fill(tx + r.textWidth(shown) + 1, y + 3, tx + r.textWidth(shown) + 2, y + 12, accent);
            }
        }
        hit(x1, y, x2, y + 16, b -> {
            resetFocus();
            searchFocused = true;
            if (b == 1) setSearch("");
        });
    }

    // ---- Modulliste ----

    private List<Module> visibleModules() {
        List<Module> base = new ArrayList<>();
        for (Module m : !search.isEmpty() || category == null ? core.modules().all() : core.modules().byCategory(category)) {
            if (m.category() != Category.CLIENT || !search.isEmpty()) base.add(m);
        }
        return core.clientSettings().sortMode.get().sort(SortMode.search(base, search));
    }

    private void renderList(Renderer r, int mx, int my, float x, float top, float w, float bottom, int accent) {
        List<Module> list = visibleModules();
        // Kopfzeile: Titel, Anzahl aktiv, rechts Alle an/aus und Sortierung
        float hy = top + 7;
        String title = !search.isEmpty() ? Lang.tr("aio.ui.menu.results", "Results") : category == null
                ? Lang.tr("aio.ui.menu.all_modules", "All modules") : category.displayName();
        int on = 0;
        for (Module m : list) if (m.isEnabled() && m.canToggle()) on++;
        Draw.bold(r, title, x + 2, hy, Theme.TEXT);
        String count = Lang.tr("aio.ui.menu.active_count", "%s of %s on", on, list.size());
        r.text(count, x + 2 + Draw.boldWidth(r, title) + 6, hy, Theme.TEXT_MUTED, false);

        float bx = x + w - 2;
        bx = ghostButton(r, mx, my, bx, hy - 3, core.clientSettings().sortMode.display(), Draw.SORT, accent,
                b -> core.clientSettings().sortMode.cycle(b == 1 ? -1 : 1)) - 4;
        bx = ghostButton(r, mx, my, bx, hy - 3, Lang.tr("aio.ui.menu.disable_all", "Disable all"), null, accent, b -> setAll(false)) - 4;
        ghostButton(r, mx, my, bx, hy - 3, Lang.tr("aio.ui.menu.enable_all", "Enable all"), null, accent, b -> setAll(true));

        float listTop = top + 22;
        listY1 = listTop;
        listY2 = bottom - 2;
        int cols = w >= 360 ? 2 : 1;
        float colW = (w - (cols - 1) * GAP) / cols;
        r.enableScissor((int) x - 1, (int) listTop, (int) (x + w + 1), (int) listY2);
        clip = 1;
        float y0 = listTop - listScroll;
        if (list.isEmpty()) {
            r.centeredText(Lang.tr("aio.ui.menu.nothing_found", "Nothing found for \"%s\"", search), x + w / 2f, listTop + 30, Theme.TEXT_MUTED, false);
        }
        for (int i = 0; i < list.size(); i++) {
            float rx = x + (i % cols) * (colW + GAP), ry = y0 + (i / cols) * (ROW_H + GAP);
            if (ry + ROW_H < listTop || ry > listY2) continue;
            moduleRow(r, mx, my, list.get(i), rx, ry, colW, accent);
        }
        clip = 0;
        r.disableScissor();
        int rows = (list.size() + cols - 1) / cols;
        listMax = Math.max(0, rows * (ROW_H + GAP) - GAP - (listY2 - listTop));
        listScroll = Math.max(0, Math.min(listMax, listScroll));
        scrollbar(r, x + w + 3, listTop, listY2, listScroll, listMax);
    }

    private void moduleRow(Renderer r, int mx, int my, Module m, float x, float y, float w, int accent) {
        boolean hover = inside(mx, my, x, y, x + w, y + ROW_H) && my >= listY1 && my <= listY2 && mx < drawerX1;
        boolean selected = drawer == m && !clientDrawer && drawerOpen();
        boolean active = m.isEnabled();
        boolean problem = m.isEnabled() && (m.isBlocked() || m.unavailableReason() != null);
        float hv = anim.to(m, hover ? 1 : 0, 20);
        Draw.round(r, x, y, x + w, y + ROW_H, 2, Colors.lerp(Theme.CARD, Theme.CARD_HOVER, hv));
        if (selected) Draw.roundOutline(r, x, y, x + w, y + ROW_H, Colors.withAlpha(accent, 0xB0));
        // Statusleiste links: Akzent wenn an, Warnfarbe bei Problem
        float on = anim.to("on" + m.id(), active ? 1 : 0, 14);
        if (on > 0.01f) {
            int c = problem ? Theme.WARNING : accent;
            float bh = (ROW_H - 10) * on;
            Draw.round(r, x + 3, y + ROW_H / 2f - bh / 2, x + 5, y + ROW_H / 2f + bh / 2, 1, c);
        }
        float tx = x + 10;
        float right = x + w - 6;
        // Schalter, davor Taste
        if (m.canToggle()) {
            right -= 18;
            Draw.toggle(r, right, y + 9, on, problem ? Theme.WARNING : accent, false);
            hit(right - 3, y + 4, right + 21, y + ROW_H - 4, b -> m.toggle());
            right -= 6;
        }
        if (m.toggleKey().isBound()) {
            String key = core.platform().keyName(m.toggleKey().key());
            float kw = r.textWidth(key) + 8;
            right -= kw;
            Draw.keycap(r, key, right, y + 8, Theme.TEXT_DIM);
            right -= 6;
        }
        r.text(r.trim(m.name(), right - tx), tx, y + 6, active ? Theme.TEXT : Colors.lerp(Theme.TEXT_DIM, Theme.TEXT, hv * 0.5f), false);
        String sub = problem ? (m.unavailableReason() != null ? m.unavailableReason() : Lang.tr("aio.ui.menu.blocked_server", "Blocked on this server"))
                : m.description();
        float sx = tx;
        if (!problem && (category == null || !search.isEmpty())) {
            // In "All" und bei der Suche: Kategorie in ihrer Farbe davor
            String cat = m.category().displayName();
            r.text(cat, sx, y + 17, Colors.withAlpha(m.category().color(), 0xC0), false);
            sx += r.textWidth(cat) + 5;
        }
        r.text(r.trim(sub, right - sx), sx, y + 17, problem ? Theme.WARNING : Theme.TEXT_MUTED, false);
        if (hover) hint = problem ? sub : m.description();
        // Zeile öffnet die Einstellungen (Schalter wird später geprüft und gewinnt)
        hits.add(hits.size() - (m.canToggle() ? 1 : 0), new Hit(x, y, x + w, y + ROW_H, clip, b -> openDrawer(m)));
    }

    private float ghostButton(Renderer r, int mx, int my, float right, float y, String label, String[] icon, int accent,
                              IntConsumer action) {
        float iw = icon == null ? 0 : 11;
        float w = r.textWidth(label) + 10 + iw, x = right - w;
        boolean hover = inside(mx, my, x, y, right, y + 14);
        if (hover) Draw.round(r, x, y, right, y + 14, 2, Theme.CARD_HOVER);
        if (icon != null) Draw.icon(r, icon, x + 5, y + 3, hover ? Theme.TEXT : Theme.TEXT_MUTED);
        r.text(label, x + 5 + iw, y + 3, hover ? Theme.TEXT : Theme.TEXT_DIM, false);
        hit(x, y, right, y + 14, action);
        return x;
    }

    private void scrollbar(Renderer r, float x, float top, float bottom, float scroll, float max) {
        if (max <= 0) return;
        float track = bottom - top, content = track + max;
        float bar = Math.max(14, track * track / content);
        float by = top + (track - bar) * (scroll / max);
        Draw.round(r, x - 1, by, x + 1, by + bar, 1, 0x50FFFFFF);
    }

    // ---- Panel rechts ----

    private boolean drawerOpen() { return drawer != null || clientDrawer; }

    private void renderDrawer(Renderer r, int mx, int my, int px, int pw, float top, float bottom, int accent) {
        float t = anim.to("drawer", drawerOpen() ? 1 : 0, 16);
        float dw = Math.min(280, Math.max(220, pw * 0.52f));
        drawerX1 = Float.MAX_VALUE;
        if (t <= 0.01f) return;
        float x1 = px + pw - dw * ease(t), x2 = x1 + dw;
        drawerX1 = x1;
        drawerY1 = top;
        drawerY2 = bottom;
        // Schatten nach links, Fläche, Kante
        for (int i = 0; i < 6; i++) r.fill(x1 - 6 + i, top, x1 - 5 + i, bottom, Colors.withAlpha(0, 6 + i * 8));
        r.fill(x1, top, Math.min(x2, px + pw - 1), bottom, 0xFF12141B);
        r.fill(x1, top, x1 + 1, bottom, Theme.LINE_STRONG);
        // Klicks im Panel gehen nicht an die Liste darunter
        hit(x1, top, x2, bottom, b -> {});
        if (!drawerOpen()) return;

        float x = x1 + 10, w = dw - 20;
        r.enableScissor((int) x1 + 1, (int) top, (int) Math.min(x2, px + pw - 1), (int) bottom);
        clip = 2;
        float y = top + 8 - drawerScroll;
        float start = y;
        y = clientDrawer ? renderClientDrawer(r, mx, my, x, y, w, accent) : renderModuleDrawer(r, mx, my, drawer, x, y, w, accent);
        clip = 0;
        r.disableScissor();
        drawerMax = Math.max(0, (y - start) + 8 - (bottom - top));
        drawerScroll = Math.max(0, Math.min(drawerMax, drawerScroll));
        scrollbar(r, x2 - 3, top + 2, bottom - 2, drawerScroll, drawerMax);
    }

    private static float ease(float t) { return 1 - (1 - t) * (1 - t) * (1 - t); }

    private float drawerHeader(Renderer r, int mx, int my, String name, String desc, float x, float y, float w, int accent) {
        // Schließen
        float cx = x + w - 12;
        boolean hover = inside(mx, my, cx - 2, y - 2, cx + 10, y + 10);
        Draw.icon(r, Draw.CLOSE, cx, y, hover ? Theme.TEXT : Theme.TEXT_MUTED);
        hit(cx - 3, y - 3, cx + 11, y + 11, b -> closeDrawer());
        Draw.bold(r, r.trim(name, w - 40), x, y, Theme.TEXT);
        y += 13;
        for (String line : wrap(r, desc, w)) {
            r.text(line, x, y, Theme.TEXT_DIM, false);
            y += 10;
        }
        return y + 4;
    }

    private float renderModuleDrawer(Renderer r, int mx, int my, Module m, float x, float y, float w, int accent) {
        y = drawerHeader(r, mx, my, m.name(), m.description(), x, y, w, accent);
        // Ein/Aus groß, mit Status
        if (m.canToggle()) {
            boolean problem = m.isEnabled() && (m.isBlocked() || m.unavailableReason() != null);
            Draw.round(r, x, y, x + w, y + 22, 2, Theme.CARD);
            String state = !m.isEnabled() ? Lang.tr("aio.ui.menu.off", "Off") : problem
                    ? Lang.tr("aio.ui.menu.on_inactive", "On, but inactive") : Lang.tr("aio.ui.menu.on", "On");
            r.text(state, x + 8, y + 7, m.isEnabled() ? (problem ? Theme.WARNING : Theme.TEXT) : Theme.TEXT_DIM, false);
            float on = anim.to("on" + m.id(), m.isEnabled() ? 1 : 0, 14);
            Draw.toggle(r, x + w - 26, y + 6, on, problem ? Theme.WARNING : accent, false);
            hit(x, y, x + w, y + 22, b -> m.toggle());
            y += 26;
        }
        if (m.unavailableReason() != null) y = notice(r, "! " + m.unavailableReason(), x, y, w, Theme.WARNING);
        else if (m.isBlocked()) y = notice(r, Lang.tr("aio.ui.menu.blocked_notice", "! Blocked on this server (compatibility mode)"), x, y, w, Theme.WARNING);
        if (m.isGameplayAffecting()) {
            y = notice(r, Lang.tr("aio.ui.menu.advantage_notice", "May count as an advantage on servers; compatibility mode blocks it."), x, y, w, Theme.TEXT_MUTED);
        }

        List<Setting<?>> general = new ArrayList<>(), look = new ArrayList<>();
        for (Setting<?> s : m.settings()) {
            if (!s.isVisible() || s == m.toggleKey() && !m.canToggle()) continue;
            (APPEARANCE.contains(s.id()) ? look : general).add(s);
        }
        if (!general.isEmpty()) {
            y = section(r, Lang.tr("aio.ui.menu.settings", "Settings"), x, y + 2, w);
            for (Setting<?> s : general) y = renderSetting(r, mx, my, s, x, y, w, accent);
        }
        if (!look.isEmpty()) {
            y += 4;
            boolean hover = inside(mx, my, x, y, x + w, y + 12);
            Draw.icon(r, appearanceOpen ? Draw.CHEVRON_DOWN : Draw.CHEVRON_RIGHT, x - 2, y + 2, hover ? Theme.TEXT : Theme.TEXT_MUTED);
            r.text(Lang.tr("aio.ui.menu.appearance", "Appearance"), x + 8, y + 2, hover ? Theme.TEXT : Theme.TEXT_DIM, false);
            hit(x, y, x + w, y + 12, b -> appearanceOpen = !appearanceOpen);
            y += 14;
            if (appearanceOpen) for (Setting<?> s : look) y = renderSetting(r, mx, my, s, x, y, w, accent);
        }
        y += 8;
        float bw = r.textWidth(Lang.tr("aio.ui.menu.reset_module", "Reset module")) + 20;
        boolean hover = inside(mx, my, x, y, x + bw, y + 14);
        Draw.round(r, x, y, x + bw, y + 14, 2, hover ? 0x30FF5C5C : Theme.CARD);
        Draw.icon(r, Draw.RESET, x + 4, y + 3, hover ? Theme.DANGER : Theme.TEXT_MUTED);
        r.text(Lang.tr("aio.ui.menu.reset_module", "Reset module"), x + 15, y + 3, hover ? Theme.DANGER : Theme.TEXT_DIM, false);
        hit(x, y, x + bw, y + 14, b -> {
            for (Setting<?> s : m.settings()) if (!s.isHidden()) s.reset();
            showStatus(Lang.tr("aio.ui.menu.module_reset", "%s reset to default", m.name()), accent);
        });
        return y + 18;
    }

    private float renderClientDrawer(Renderer r, int mx, int my, float x, float y, float w, int accent) {
        Module client = core.clientSettings();
        y = drawerHeader(r, mx, my, Lang.tr("aio.ui.menu.client_title", "Client settings"),
                Lang.tr("aio.ui.menu.client_desc", "Menu key, look, server compatibility and profiles."), x, y, w, accent);
        y = section(r, Lang.tr("aio.ui.menu.general", "General"), x, y, w);
        for (Setting<?> s : client.settings()) {
            if (s.isVisible() && s != client.toggleKey()) y = renderSetting(r, mx, my, s, x, y, w, accent);
        }

        // Profile
        y = section(r, Lang.tr("aio.ui.menu.profiles", "Profiles"), x, y + 6, w);
        ConfigManager cfg = core.config();
        for (String p : cfg.profiles()) {
            boolean active = p.equals(cfg.activeProfile());
            boolean hover = inside(mx, my, x, y, x + w, y + 16) && inClip(my);
            Draw.round(r, x, y, x + w, y + 16, 2, active ? Colors.withAlpha(accent, 0x30) : hover ? Theme.CARD_HOVER : Theme.CARD);
            r.text(r.trim(p, w - 50), x + 8, y + 4, active ? Theme.TEXT : Theme.TEXT_DIM, false);
            if (active) r.text(Lang.tr("aio.ui.menu.active", "active"), x + w - 8 - r.textWidth(Lang.tr("aio.ui.menu.active", "active")), y + 4, accent, false);
            hit(x, y, x + w, y + 16, b -> {
                if (!active) {
                    cfg.switchProfile(p);
                    showStatus(Lang.tr("aio.ui.menu.profile_switched", "Profile: %s", p), accent);
                }
            });
            y += 18;
        }
        float bw = (w - 9) / 4f;
        String[] labels = {Lang.tr("aio.ui.menu.new", "New"), Lang.tr("aio.ui.menu.export", "Export"), Lang.tr("aio.ui.menu.import", "Import"),
                confirmDelete ? Lang.tr("aio.ui.menu.sure", "Sure?") : Lang.tr("aio.ui.menu.delete", "Delete")};
        IntConsumer[] actions = {
                b -> showStatus(Lang.tr("aio.ui.menu.profile_created", "Profile \"%s\" created", cfg.createProfile("profile")), accent),
                b -> {
                    core.platform().setClipboard(cfg.exportProfile());
                    showStatus(Lang.tr("aio.ui.menu.profile_exported", "Profile copied to clipboard and saved to exports/"), accent);
                },
                b -> {
                    try {
                        showStatus(Lang.tr("aio.ui.menu.profile_imported", "Profile \"%s\" imported", cfg.importProfile(core.platform().clipboard())), accent);
                    } catch (IllegalArgumentException e) {
                        showStatus(Lang.tr("aio.ui.menu.import_failed", "Import failed: %s", e.getMessage()), Theme.DANGER);
                    }
                },
                b -> deleteActiveProfile()};
        for (int i = 0; i < 4; i++) {
            float bx = x + i * (bw + 3);
            boolean danger = i == 3;
            boolean hover = inside(mx, my, bx, y, bx + bw, y + 14) && inClip(my);
            Draw.round(r, bx, y, bx + bw, y + 14, 2, hover ? (danger ? 0x40FF5C5C : Theme.CARD_HOVER) : Theme.CARD);
            r.centeredText(r.trim(labels[i], bw - 4), bx + bw / 2f, y + 3,
                    danger && (hover || confirmDelete) ? Theme.DANGER : hover ? Theme.TEXT : Theme.TEXT_DIM, false);
            hit(bx, y, bx + bw, y + 14, actions[i]);
        }
        y += 22;

        // Alles zurücksetzen
        y = section(r, Lang.tr("aio.ui.menu.danger_zone", "Danger zone"), x, y + 2, w);
        boolean armed = System.currentTimeMillis() - resetAllArmed < 4000;
        boolean hover = inside(mx, my, x, y, x + w, y + 18) && inClip(my);
        Draw.round(r, x, y, x + w, y + 18, 2, armed ? 0x60FF5C5C : hover ? 0x30FF5C5C : 0x14FF5C5C);
        Draw.roundOutline(r, x, y, x + w, y + 18, Colors.withAlpha(Theme.DANGER, 0xA0));
        r.centeredText(armed ? Lang.tr("aio.ui.menu.reset_all_confirm", "Click again to reset everything")
                : Lang.tr("aio.ui.menu.reset_all", "Reset all"), x + w / 2f, y + 5, Theme.DANGER, false);
        hit(x, y, x + w, y + 18, b -> {
            if (System.currentTimeMillis() - resetAllArmed < 4000) {
                resetAllArmed = 0;
                core.resetAll();
                showStatus(Lang.tr("aio.ui.menu.reset_all_done", "Everything reset to default"), Theme.DANGER);
            } else {
                resetAllArmed = System.currentTimeMillis();
            }
        });
        y += 21;
        for (String line : wrap(r, Lang.tr("aio.ui.menu.reset_all_desc", "Modules, settings, keys and HUD positions. Waypoints stay."), w)) {
            r.text(line, x, y, Theme.TEXT_MUTED, false);
            y += 10;
        }
        return y;
    }

    private float section(Renderer r, String label, float x, float y, float w) {
        String upper = label.toUpperCase(java.util.Locale.ROOT);
        r.push();
        r.translate(x, y + 2);
        r.scale(0.75f);
        r.text(upper, 0, 0, Theme.TEXT_MUTED, false);
        r.pop();
        float lx = x + r.textWidth(upper) * 0.75f + 6;
        r.fill(lx, y + 5, x + w, y + 6, Theme.LINE);
        return y + 12;
    }

    private float notice(Renderer r, String text, float x, float y, float w, int color) {
        for (String line : wrap(r, text, w)) {
            r.text(line, x, y, color, false);
            y += 10;
        }
        return y + 3;
    }

    private static List<String> wrap(Renderer r, String text, float w) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String next = line.isEmpty() ? word : line + " " + word;
            if (r.textWidth(next) > w && !line.isEmpty()) {
                out.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(next);
            }
        }
        if (!line.isEmpty()) out.add(line.toString());
        return out;
    }

    // ---- Einstellungszeilen ----

    private float renderSetting(Renderer r, int mx, int my, Setting<?> s, float x, float y, float w, int accent) {
        float right = x + w, ty = y + 5;
        boolean hover = inside(mx, my, x, y, right, y + SET_H) && inClip(my);
        if (hover) Draw.round(r, x - 4, y, right + 4, y + SET_H, 2, 0x0CFFFFFF);
        // Kleine Bedienelemente (Schalter, Taste, Farbe) lassen der Beschriftung fast die ganze Breite
        float labelW = s instanceof BooleanSetting || s instanceof KeybindSetting ? w - 50
                : s instanceof ColorSetting ? w - 80 : s instanceof StringSetting ? w * 0.42f : w * 0.5f;
        r.text(r.trim(s.name(), labelW), x, ty, hover ? Theme.TEXT : 0xFFC9CDD8, false);

        if (s instanceof ActionSetting a) {
            String label = Lang.tr("aio.ui.menu.open", "Open");
            float bw = r.textWidth(label) + 14, bx = right - bw;
            boolean bh = inside(mx, my, bx, y + 2, right, y + SET_H - 2);
            Draw.round(r, bx, y + 2, right, y + SET_H - 2, 2, bh ? brighten(accent) : accent);
            r.centeredText(label, bx + bw / 2f, ty, 0xFF101114, false);
            hit(bx, y + 2, right, y + SET_H - 2, b -> a.run());
        } else if (s instanceof BooleanSetting b) {
            float on = anim.to(b, b.on() ? 1 : 0, 14);
            Draw.toggle(r, right - 18, y + 4, on, accent, false);
            hit(x, y, right, y + SET_H, btn -> b.toggle());
        } else if (s instanceof NumberSetting n) {
            String v = n.display();
            float vw = Math.max(30, r.textWidth(v) + 8);
            Draw.round(r, right - vw, y + 3, right, y + SET_H - 3, 2, Theme.CARD);
            r.centeredText(v, right - vw / 2f, ty, Theme.TEXT_DIM, false);
            slider(r, mx, my, x + w * 0.5f, right - vw - 8, y + SET_H / 2f, n.fraction(), accent, n::setFraction, null);
        } else if (s instanceof EnumSetting<?> e) {
            enumControl(r, mx, my, e, x, y, right, accent);
        } else if (s instanceof KeybindSetting k) {
            boolean listen = listening == k;
            String v = listen ? Lang.tr("aio.ui.menu.press_key", "Press a key…")
                    : k.isBound() ? core.platform().keyName(k.key()) : Lang.tr("aio.ui.menu.none", "None");
            float kw = r.textWidth(v) + 8;
            if (listen) {
                float pulse = (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 150.0));
                Draw.round(r, right - kw, y + 3, right, y + SET_H - 3, 2, Colors.withAlpha(accent, (int) (0x60 + 0x60 * pulse)));
                r.text(v, right - kw + 4, ty, Theme.TEXT, false);
            } else {
                Draw.keycap(r, v, right - kw, y + 3, k.isBound() ? Theme.TEXT : Theme.TEXT_MUTED);
            }
            hit(right - kw - 2, y, right, y + SET_H, btn -> {
                if (listening == k && btn >= 2) { // Mittel- und Seitentasten lassen sich belegen
                    k.set(Keys.mouse(btn));
                    listening = null;
                } else if (btn == 1 && listening != k) {
                    k.set(Keys.NONE); // Rechtsklick löscht
                } else {
                    resetFocus();
                    listening = k;
                }
            });
        } else if (s instanceof StringSetting str) {
            textField(r, mx, my, str, x + w * 0.45f, y + 2, right, y + SET_H - 2, accent);
        } else if (s instanceof ColorSetting c) {
            y = colorControl(r, mx, my, c, x, y, w, accent);
            return y;
        }
        return y + SET_H;
    }

    private void enumControl(Renderer r, int mx, int my, EnumSetting<?> e, float x, float y, float right, int accent) {
        Enum<?>[] values = e.values();
        float total = 0;
        for (Enum<?> v : values) total += r.textWidth(EnumSetting.label(v)) + 10;
        float avail = right - (x + r.textWidth(e.name()) + 10);
        if (values.length <= 3 && total <= avail) {
            // Segmente: alle Werte sichtbar, der gewählte hervorgehoben
            float sx = right - total;
            Draw.round(r, sx - 1, y + 2, right + 1, y + SET_H - 2, 2, Theme.CARD);
            for (Enum<?> v : values) {
                String label = EnumSetting.label(v);
                float sw = r.textWidth(label) + 10;
                boolean sel = e.get() == v;
                boolean hover = inside(mx, my, sx, y + 2, sx + sw, y + SET_H - 2);
                if (sel) Draw.round(r, sx, y + 3, sx + sw, y + SET_H - 3, 2, Colors.withAlpha(accent, 0xD0));
                r.text(label, sx + 5, y + 5, sel ? 0xFF101114 : hover ? Theme.TEXT : Theme.TEXT_DIM, false);
                final Enum<?> value = v;
                hit(sx, y + 2, sx + sw, y + SET_H - 2, b -> selectEnum(e, value));
                sx += sw;
            }
            return;
        }
        String label = e.display();
        float cw = r.textWidth(label) + 22, cx = right - cw;
        boolean hover = inside(mx, my, cx, y + 2, right, y + SET_H - 2);
        Draw.round(r, cx, y + 2, right, y + SET_H - 2, 2, hover ? Theme.CARD_HOVER : Theme.CARD);
        Draw.icon(r, Draw.CHEVRON_LEFT, cx + 2, y + 5, Theme.TEXT_MUTED);
        r.text(label, cx + 11, y + 5, Theme.TEXT, false);
        Draw.icon(r, Draw.CHEVRON_RIGHT, right - 10, y + 5, Theme.TEXT_MUTED);
        hit(x, y, right, y + SET_H, b -> e.cycle(b == 1 ? -1 : 1));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void selectEnum(EnumSetting e, Enum value) { e.set(value); }

    private void textField(Renderer r, int mx, int my, StringSetting str, float x1, float y1, float x2, float y2, int accent) {
        boolean focused = editing == str;
        Draw.round(r, x1, y1, x2, y2, 2, focused ? 0xFF1E2230 : Theme.CARD);
        if (focused) Draw.roundOutline(r, x1, y1, x2, y2, Colors.withAlpha(accent, 0xC0));
        String shown = focused ? editBuffer : str.get();
        if (shown.isEmpty() && !focused) shown = "—";
        String vis = shown;
        float maxW = x2 - x1 - 10;
        while (r.textWidth(vis) > maxW && vis.length() > 1) vis = vis.substring(1);
        if (focused && editAll) TextEdit.drawSelection(r, x1 + 4, y1 + 3, vis);
        r.text(vis, x1 + 4, y1 + 3, focused ? Theme.TEXT : Theme.TEXT_DIM, false);
        if (focused && !editAll && (System.currentTimeMillis() / 500) % 2 == 0) {
            r.fill(x1 + 5 + r.textWidth(vis), y1 + 2, x1 + 6 + r.textWidth(vis), y2 - 2, accent);
        }
        hit(x1, y1, x2, y2, b -> {
            if (editing != str) {
                resetFocus();
                editing = str;
                editBuffer = str.get();
                editAll = false;
            }
        });
    }

    private float colorControl(Renderer r, int mx, int my, ColorSetting c, float x, float y, float w, int accent) {
        float right = x + w;
        float sw = 22, sx = right - sw;
        // Schachbrett für Transparenz, darüber die Farbe
        Draw.round(r, sx, y + 4, right, y + SET_H - 4, 2, 0xFFBBBBBB);
        r.fill(sx + 2, y + 4, sx + sw / 2, y + SET_H / 2f, 0xFF7A7A7A);
        r.fill(sx + sw / 2, y + SET_H / 2f, right - 2, y + SET_H - 4, 0xFF7A7A7A);
        Draw.round(r, sx, y + 4, right, y + SET_H - 4, 2, c.argb());
        String hex = String.format("#%06X", c.argb() & 0xFFFFFF);
        r.text(hex, sx - 6 - r.textWidth(hex), y + 5, Theme.TEXT_MUTED, false);
        hit(x, y, right, y + SET_H, b -> expandedColor = expandedColor == c ? null : c);
        y += SET_H;
        if (expandedColor == c) {
            String[] labels = {"R", "G", "B", "A"};
            int[] shifts = {16, 8, 0, 24};
            for (int i = 0; i < 4; i++) {
                int shift = shifts[i];
                int base = c.argb() | 0xFF000000;
                int from = shift == 24 ? 0x00FFFFFF & base : base & ~(0xFF << shift);
                int to = shift == 24 ? base : base | (0xFF << shift);
                r.text(labels[i], x + 8, y + 3, Theme.TEXT_MUTED, false);
                String v = String.valueOf(c.channel(shift));
                r.text(v, right - r.textWidth(v), y + 3, Theme.TEXT_DIM, false);
                slider(r, mx, my, x + 20, right - 24, y + 7, c.channel(shift) / 255.0, accent,
                        f -> c.setChannel(shift, (int) Math.round(f * 255)), new int[]{from | 0xFF000000, to | 0xFF000000});
                y += 14;
            }
            y += 2;
        }
        return y;
    }

    /** Schieberegler; {@code gradient} färbt die Spur von links nach rechts (Farbwahl). */
    private void slider(Renderer r, int mx, int my, float x1, float x2, float cy, double fraction, int accent,
                        DoubleConsumer setter, int[] gradient) {
        if (x2 - x1 < 12) return;
        float f = (float) Math.max(0, Math.min(1, fraction));
        float fx = x1 + (x2 - x1) * f;
        if (gradient != null) {
            int steps = 24;
            for (int i = 0; i < steps; i++) {
                float a = x1 + (x2 - x1) * i / steps, b = x1 + (x2 - x1) * (i + 1) / steps;
                r.fill(a, cy - 1, b, cy + 2, Colors.lerp(gradient[0], gradient[1], (i + 0.5f) / steps));
            }
        } else {
            Draw.round(r, x1, cy - 1, x2, cy + 1, 1, Theme.TRACK);
            Draw.round(r, x1, cy - 1, Math.max(x1 + 2, fx), cy + 1, 1, accent);
        }
        boolean hover = inside(mx, my, x1 - 3, cy - 6, x2 + 3, cy + 6);
        int knob = hover || slider != null ? 0xFFFFFFFF : 0xFFDADDE5;
        Draw.round(r, fx - 3, cy - 3, fx + 3, cy + 3, 2, knob);
        hit(x1 - 4, cy - 7, x2 + 4, cy + 7, b -> {
            if (b == 0) slider = new SliderDrag(x1, x2, setter);
        });
    }

    // ---- Untere Leiste ----

    private void renderBottomBar(Renderer r, int mx, int my, int px, float y, int pw, int accent) {
        r.fill(px + 1, y, px + pw - 1, y + 1, Theme.LINE);
        ConfigManager cfg = core.config();
        float x = px + 8, ty = y + 5;
        // Profil wechseln
        boolean multi = cfg.profiles().size() > 1;
        if (multi) {
            boolean hl = inside(mx, my, x - 2, y + 2, x + 9, y + BOTTOM - 2);
            Draw.icon(r, Draw.CHEVRON_LEFT, x, ty, hl ? Theme.TEXT : Theme.TEXT_MUTED);
            hit(x - 2, y + 2, x + 9, y + BOTTOM - 2, b -> cycleProfile(-1));
            x += 10;
        }
        String profile = Lang.tr("aio.ui.menu.profile", "Profile: %s", cfg.activeProfile());
        r.text(profile, x, ty, Theme.TEXT_DIM, false);
        x += r.textWidth(profile) + 2;
        if (multi) {
            boolean hr = inside(mx, my, x, y + 2, x + 11, y + BOTTOM - 2);
            Draw.icon(r, Draw.CHEVRON_RIGHT, x, ty, hr ? Theme.TEXT : Theme.TEXT_MUTED);
            hit(x, y + 2, x + 11, y + BOTTOM - 2, b -> cycleProfile(1));
        }

        // Ganz rechts: Schließen-Knopf mit rotem Rand; links davon Meldung oder Beschreibung
        String label = Lang.tr("aio.ui.menu.close", "Close") + "  Esc";
        float bw = 8 + 5 + r.textWidth(label) + 12, bx2 = px + pw - 6, bx1 = bx2 - bw, by1 = y + 2, by2 = y + BOTTOM - 2;
        boolean hv = inside(mx, my, bx1, by1, bx2, by2);
        Draw.round(r, bx1, by1, bx2, by2, 2, hv ? Colors.withAlpha(Theme.DANGER, 0x40) : 0x00000000);
        Draw.roundOutline(r, bx1, by1, bx2, by2, Colors.withAlpha(Theme.DANGER, hv ? 0xFF : 0xA0));
        Draw.icon(r, Draw.CLOSE, bx1 + 5, ty - 1, hv ? Theme.DANGER : Colors.withAlpha(Theme.DANGER, 0xCC));
        r.text(label, bx1 + 18, ty, hv ? Theme.TEXT : Theme.TEXT_DIM, false);
        hit(bx1, by1, bx2, by2, b -> core.platform().closeScreen());

        String text = null;
        int color = Theme.TEXT_DIM;
        if (status != null && System.currentTimeMillis() < statusUntil) {
            text = status;
            color = statusColor;
        } else if (hint != null) {
            text = hint;
        }
        if (text != null) {
            float maxW = bx1 - 14 - (x + 20);
            String shown = r.trim(text, maxW);
            r.text(shown, bx1 - 14 - r.textWidth(shown), ty, color, false);
        }
    }

    // =====================================================================
    // Aktionen
    // =====================================================================

    private void selectCategory(Category c) {
        category = c;
        listScroll = 0;
        setSearch("");
        resetFocus();
    }

    private void setAll(boolean on) {
        for (Module m : visibleModules()) if (m.canToggle()) m.setEnabled(on);
    }

    private void openDrawer(Module m) {
        resetFocus();
        if (drawer != m || clientDrawer) drawerScroll = 0;
        drawer = m;
        clientDrawer = false;
        appearanceOpen = false;
    }

    private void toggleClientDrawer() {
        resetFocus();
        if (clientDrawer) {
            closeDrawer();
            return;
        }
        clientDrawer = true;
        drawer = null;
        drawerScroll = 0;
    }

    private void closeDrawer() {
        resetFocus();
        drawer = null;
        clientDrawer = false;
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
        showStatus(Lang.tr("aio.ui.menu.profile_switched", "Profile: %s", next), Theme.accent(core));
    }

    private void deleteActiveProfile() {
        deleteTouched = true;
        ConfigManager cfg = core.config();
        String active = cfg.activeProfile();
        List<String> profiles = cfg.profiles();
        if (profiles.size() < 2) {
            showStatus(Lang.tr("aio.ui.menu.last_profile", "The last profile cannot be deleted"), Theme.DANGER);
            return;
        }
        if (!confirmDelete) {
            confirmDelete = true;
            return;
        }
        confirmDelete = false;
        String fallback = profiles.contains(ConfigManager.DEFAULT_PROFILE) && !active.equals(ConfigManager.DEFAULT_PROFILE)
                ? ConfigManager.DEFAULT_PROFILE : profiles.get(profiles.indexOf(active) == 0 ? 1 : 0);
        cfg.switchProfile(fallback);
        if (cfg.deleteProfile(active)) showStatus(Lang.tr("aio.ui.menu.profile_deleted", "Profile \"%s\" deleted", active), Theme.accent(core));
    }

    private void showStatus(String message, int color) {
        status = message;
        statusColor = color;
        statusUntil = System.currentTimeMillis() + 3000;
    }

    // =====================================================================
    // Hilfen
    // =====================================================================

    private void hit(float x1, float y1, float x2, float y2, IntConsumer action) {
        hits.add(new Hit(x1, y1, x2, y2, clip, action));
    }

    private boolean inClip(double my) {
        return clip == 1 ? my >= listY1 && my <= listY2 : clip != 2 || (my >= drawerY1 && my <= drawerY2);
    }

    private static boolean inside(double mx, double my, float x1, float y1, float x2, float y2) {
        return mx >= x1 && mx <= x2 && my >= y1 && my <= y2;
    }

    private static int brighten(int c) {
        return Colors.lerp(c, 0xFFFFFFFF, 0.15f) | (c & 0xFF000000);
    }

    // =====================================================================
    // Eingaben
    // =====================================================================

    @Override public boolean mouseClicked(double mx, double my, int button) {
        boolean wasConfirming = confirmDelete;
        deleteTouched = false;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (!h.contains(mx, my) || !clickable(h, my)) continue;
            commitEdit();
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

    private boolean clickable(Hit h, double my) {
        return switch (h.clip) {
            case 1 -> my >= listY1 && my <= listY2;
            case 2 -> my >= drawerY1 && my <= drawerY2;
            default -> true;
        };
    }

    @Override public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (slider == null) return false;
        slider.apply(mx);
        return true;
    }

    @Override public boolean mouseReleased(double mx, double my, int button) {
        if (slider == null) return false;
        slider = null;
        return true;
    }

    @Override public boolean mouseScrolled(double mx, double my, double amount) {
        if (drawerOpen() && mx >= drawerX1) drawerScroll = (float) Math.max(0, Math.min(drawerMax, drawerScroll - amount * 20));
        else listScroll = (float) Math.max(0, Math.min(listMax, listScroll - amount * 20));
        return true;
    }

    @Override public boolean keyPressed(String key) {
        swallowChar = false;
        if (listening != null) {
            listening.set(Keys.ESCAPE.equals(key) || Keys.BACKSPACE.equals(key) || Keys.DELETE.equals(key) ? Keys.NONE : key);
            listening = null;
            swallowChar = true;
            return true;
        }
        if (editing != null) {
            if (Keys.ESCAPE.equals(key)) {
                editing = null;
                editAll = false;
            } else if (Keys.ENTER.equals(key)) {
                editAll = false;
                commitEdit();
            } else {
                TextEdit.Result res = TextEdit.key(core.platform(), editBuffer, editAll, key);
                editBuffer = res.text().length() > editing.maxLength() ? res.text().substring(0, editing.maxLength()) : res.text();
                editAll = res.allSelected();
            }
            return true;
        }
        if (searchFocused) {
            if (Keys.ESCAPE.equals(key) || Keys.ENTER.equals(key)) {
                searchFocused = false;
                searchAll = false;
            } else {
                TextEdit.Result res = TextEdit.key(core.platform(), search, searchAll, key);
                if (!res.text().equals(search)) setSearch(res.text());
                searchAll = res.allSelected();
            }
            return true;
        }
        if ("key.keyboard.f".equals(key) && TextEdit.ctrl(core.platform())) {
            searchFocused = true;
            return true;
        }
        if (Keys.ESCAPE.equals(key) && drawerOpen()) {
            closeDrawer();
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
        listScroll = 0;
    }

    @Override public boolean charTyped(String text) {
        if (text == null || text.isEmpty()) return false;
        if (swallowChar) {
            swallowChar = false;
            return true;
        }
        int cp = text.codePointAt(0);
        if (editing != null) {
            if (cp < 32 || cp == 127) return false;
            editBuffer = TextEdit.type(editBuffer, editAll, text.replaceAll("[\\r\\n\\t]", ""));
            editAll = false;
            if (editBuffer.length() > editing.maxLength()) editBuffer = editBuffer.substring(0, editing.maxLength());
            return true;
        }
        if (listening != null) return false;
        // Einfach lostippen: landet in der Suche
        if (!searchFocused && cp > 32) searchFocused = true;
        if (searchFocused && cp >= 32) {
            setSearch(TextEdit.type(search, searchAll, text));
            searchAll = false;
            return true;
        }
        return false;
    }

    /** Aus dem HUD-Editor (Rechtsklick) geöffnet: ESC führt zurück in den Editor. */
    @Override public boolean returnsToPrevious() { return true; }

    @Override public void onClose() {
        commitEdit();
        core.config().save();
        memory = new Memory(core, category, search, listScroll, drawer != null ? drawer.id() : null, clientDrawer,
                drawerScroll, appearanceOpen);
    }

    private void restore(Memory m) {
        category = m.category();
        search = m.search();
        listScroll = m.listScroll();
        // Alte Suche markiert: einfach lostippen ersetzt sie
        if (!search.isEmpty()) {
            searchFocused = true;
            searchAll = true;
        }
        Module d = m.drawerId() != null ? core.modules().get(m.drawerId()) : null;
        if (m.clientDrawer()) clientDrawer = true;
        else if (d != null) drawer = d;
        if (drawerOpen()) {
            anim.set("drawer", 1);
            drawerScroll = m.drawerScroll();
            appearanceOpen = m.appearanceOpen();
        }
    }
}
