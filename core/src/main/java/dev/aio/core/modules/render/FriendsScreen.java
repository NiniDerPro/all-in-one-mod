package dev.aio.core.modules.render;

import dev.aio.core.AioCore;
import dev.aio.core.i18n.Lang;
import dev.aio.core.modules.render.FriendsModule.Match;
import dev.aio.core.modules.render.FriendsModule.Rule;
import dev.aio.core.platform.Keys;
import dev.aio.core.platform.Renderer;
import dev.aio.core.ui.Draw;
import dev.aio.core.ui.TextEdit;
import dev.aio.core.ui.Theme;
import dev.aio.core.ui.UiScreen;
import dev.aio.core.ui.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * List of friends and name rules: new rule at the top (type, text, colour, +), below every rule with
 * colour, type, text, on/off and delete. Typing goes straight into the new-rule field.
 */
public final class FriendsScreen implements UiScreen {
    private record Hit(float x1, float y1, float x2, float y2, IntConsumer action) {}

    private static final int ROW = 22;
    private static final int CHIP = 12;

    private final AioCore core;
    private final FriendsModule module;
    private final List<Hit> hits = new ArrayList<>();
    private float scroll;

    // new rule
    private Match newMatch = Match.NAME;
    private String input = "";
    private boolean inputAll;
    private int newColor = Match.NAME.defaultColor();
    private boolean colorPicked;

    // editing an existing rule's text
    private Rule editing;
    private String buffer = "";
    private boolean bufferAll;

    private Rule confirmDelete;
    /** Open colour picker: for a rule, or for the new rule when {@link #pickNew} is set. */
    private Rule pickFor;
    private boolean pickNew;
    private float pickX, pickY;

    public FriendsScreen(AioCore core, FriendsModule module) {
        this.core = core;
        this.module = module;
    }

    @Override public String title() { return Lang.tr("aio.ui.friends.title", "Friends"); }

    @Override public void render(Renderer r, int mx, int my, float delta) {
        hits.clear();
        int w = r.screenWidth(), h = r.screenHeight();
        r.fill(0, 0, w, h, Theme.OVERLAY);
        int pw = Math.min(w - 20, 440), ph = Math.min(h - 20, 270);
        int px = (w - pw) / 2, py = (h - ph) / 2;
        Window.frame(r, px, py, px + pw, py + ph);
        Window.title(r, Lang.tr("aio.ui.friends.title", "Friends"), px + 10, py + 9);
        String hint = Lang.tr("aio.ui.friends.hint", "First matching rule sets the colour");
        r.text(hint, px + pw - 10 - r.textWidth(hint), py + 9, Theme.TEXT_MUTED, false);

        drawInputRow(r, mx, my, px + 8, py + 26, pw - 16);

        List<Rule> list = module.rules();
        int top = py + 52, bottom = py + ph - 8;
        r.enableScissor(px, top, px + pw, bottom);
        float y = top - scroll;
        if (list.isEmpty()) {
            r.text(Lang.tr("aio.ui.friends.empty", "No entries yet. Type a name above and press Enter or +."), px + 10, top + 6, Theme.TEXT_DIM, false);
        }
        for (Rule rule : list) {
            if (y + ROW >= top && y <= bottom) drawRow(r, mx, my, rule, px + 8, y, pw - 16, top, bottom);
            y += ROW + 2;
        }
        r.disableScissor();
        float max = Math.max(0, list.size() * (ROW + 2) - (bottom - top));
        scroll = Math.max(0, Math.min(max, scroll));

        if (pickNew || pickFor != null) drawPicker(r, mx, my);
    }

    private void drawInputRow(Renderer r, int mx, int my, float x, float y, float w) {
        Draw.round(r, x, y, x + w, y + ROW, 2, Theme.CARD);
        // type
        float tx = x + 4;
        button(r, mx, my, tx, y + 4, tx + 92, y + 18, newMatch.label(), false, b -> {
            newMatch = b == 1 ? prev(newMatch) : newMatch.next();
            if (!colorPicked) newColor = newMatch.defaultColor();
        });
        // text field
        float fx = tx + 98, fx2 = x + w - 52;
        boolean focus = editing == null;
        r.fill(fx, y + 4, fx2, y + 18, focus ? 0xFF2A2A34 : 0xFF202028);
        String visible = r.trim(input, (int) (fx2 - fx - 10));
        if (input.isEmpty()) {
            r.text(newMatch == Match.NAME ? Lang.tr("aio.ui.friends.name_placeholder", "Player name")
                    : Lang.tr("aio.ui.friends.text_placeholder", "Text in the name"), fx + 4, y + 7, Theme.TEXT_MUTED, false);
        }
        if (inputAll) TextEdit.drawSelection(r, fx + 4, y + 7, visible);
        boolean caret = focus && !inputAll && (System.currentTimeMillis() / 500) % 2 == 0;
        r.text(visible + (caret ? "_" : ""), fx + 4, y + 7, Theme.TEXT, false);
        hits.add(new Hit(fx, y + 4, fx2, y + 18, b -> commitEdit()));
        // colour
        float cx = x + w - 46;
        swatch(r, cx, y + 5, newColor, b -> {
            pickNew = true;
            pickFor = null;
            pickX = cx;
            pickY = y + 19;
        });
        // add
        float ax = x + w - 28;
        boolean hover = mx >= ax && mx <= ax + 24 && my >= y + 4 && my <= y + 18;
        Draw.round(r, ax, y + 4, ax + 24, y + 18, 2, input.isBlank() ? 0xFF24242C : hover ? 0xFF33333E : Theme.accent(core));
        Draw.icon(r, Draw.PLUS, ax + 8, y + 7, Theme.TEXT);
        hits.add(new Hit(ax, y + 4, ax + 24, y + 18, b -> addInput()));
    }

    private void drawRow(Renderer r, int mx, int my, Rule rule, float x, float y, float w, int top, int bottom) {
        Draw.round(r, x, y, x + w, y + ROW, 2, Theme.CARD);
        float sx = x + 6;
        swatch(r, sx, y + 5, rule.color(), null);
        hitClipped(sx, y + 5, sx + CHIP, y + 5 + CHIP, top, bottom, b -> {
            pickFor = rule;
            pickNew = false;
            pickX = sx;
            pickY = y + 19;
        });
        float tx = x + 24;
        button(r, mx, my, tx, y + 4, tx + 92, y + 18, rule.match().label(), false,
                b -> module.replace(rule, rule.withMatch(b == 1 ? prev(rule.match()) : rule.match().next())), top, bottom);

        float nx = tx + 100, nx2 = x + w - 80;
        if (editing == rule) {
            r.fill(nx - 2, y + 4, nx2, y + 18, 0xFF2A2A34);
            String visible = r.trim(buffer, (int) (nx2 - nx - 8));
            if (bufferAll) TextEdit.drawSelection(r, nx, y + 7, visible);
            boolean caret = !bufferAll && (System.currentTimeMillis() / 500) % 2 == 0;
            r.text(visible + (caret ? "_" : ""), nx, y + 7, Theme.TEXT, false);
        } else {
            int color = rule.enabled() ? rule.color() : Theme.TEXT_MUTED;
            r.text(r.trim(rule.text(), (int) (nx2 - nx - 4)), nx, y + 7, color, false);
            hitClipped(nx - 2, y + 4, nx2, y + 18, top, bottom, b -> {
                commitEdit();
                editing = rule;
                buffer = rule.text();
                bufferAll = false;
            });
        }
        button(r, mx, my, x + w - 74, y + 4, x + w - 34, y + 18,
                rule.enabled() ? Lang.tr("aio.ui.waypoint.on", "On") : Lang.tr("aio.ui.waypoint.off", "Off"), rule.enabled(),
                b -> module.replace(rule, rule.withEnabled(!rule.enabled())), top, bottom);
        boolean confirm = confirmDelete == rule;
        float dx = x + w - 30;
        boolean hover = mx >= dx && mx <= dx + 24 && my >= y + 4 && my <= y + 18;
        Draw.round(r, dx, y + 4, dx + 24, y + 18, 2, confirm ? Theme.DANGER : hover ? 0xFF33333E : 0xFF24242C);
        Draw.icon(r, Draw.TRASH, dx + 8, y + 7, confirm ? 0xFFFFFFFF : Theme.DANGER);
        hitClipped(dx, y + 4, dx + 24, y + 18, top, bottom, b -> {
            if (armed == rule) {
                if (editing == rule) editing = null;
                module.remove(rule);
            } else {
                confirmDelete = rule;
            }
        });
    }

    private void drawPicker(Renderer r, int mx, int my) {
        int n = FriendsModule.PALETTE.length;
        float w = n * (CHIP + 4) + 6, h = CHIP + 8;
        float x = Math.min(pickX, r.screenWidth() - w - 4), y = pickY;
        Draw.round(r, x, y, x + w, y + h, 2, Theme.SURFACE);
        Draw.roundOutline(r, x, y, x + w, y + h, Theme.LINE_STRONG);
        int current = pickNew ? newColor : pickFor.color();
        boolean forNew = pickNew;
        Rule target = pickFor;
        for (int i = 0; i < n; i++) {
            int c = FriendsModule.PALETTE[i];
            float cx = x + 4 + i * (CHIP + 4), cy = y + 4;
            if (c == current) r.fill(cx - 1, cy - 1, cx + CHIP + 1, cy + CHIP + 1, 0xFFFFFFFF);
            r.fill(cx, cy, cx + CHIP, cy + CHIP, c);
            hits.add(new Hit(cx - 1, cy - 1, cx + CHIP + 1, cy + CHIP + 1, b -> {
                if (forNew) {
                    newColor = c;
                    colorPicked = true;
                } else if (target != null) {
                    module.replace(target, target.withColor(c));
                }
            }));
        }
    }

    private void swatch(Renderer r, float x, float y, int color, IntConsumer action) {
        r.fill(x - 1, y - 1, x + CHIP + 1, y + CHIP + 1, 0xFF000000);
        r.fill(x, y, x + CHIP, y + CHIP, color | 0xFF000000);
        if (action != null) hits.add(new Hit(x - 1, y - 1, x + CHIP + 1, y + CHIP + 1, action));
    }

    private static Match prev(Match m) {
        Match[] v = Match.values();
        return v[(m.ordinal() + v.length - 1) % v.length];
    }

    private void closePicker() {
        pickFor = null;
        pickNew = false;
    }

    private void addInput() {
        String text = input.trim();
        if (text.isEmpty()) return;
        module.addRule(new Rule(newMatch, text, newColor, true));
        input = "";
        inputAll = false;
    }

    private void button(Renderer r, int mx, int my, float x1, float y1, float x2, float y2, String label, boolean accent, IntConsumer action) {
        button(r, mx, my, x1, y1, x2, y2, label, accent, action, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    private void button(Renderer r, int mx, int my, float x1, float y1, float x2, float y2, String label, boolean accent,
                        IntConsumer action, int top, int bottom) {
        boolean hover = mx >= x1 && mx <= x2 && my >= y1 && my <= y2;
        Draw.round(r, x1, y1, x2, y2, 2, accent ? Theme.accent(core) : hover ? 0xFF33333E : 0xFF24242C);
        r.centeredText(r.trim(label, (int) (x2 - x1 - 4)), (x1 + x2) / 2, y1 + (y2 - y1 - 8) / 2, Theme.TEXT, false);
        hitClipped(x1, y1, x2, y2, top, bottom, action);
    }

    private void hitClipped(float x1, float y1, float x2, float y2, int top, int bottom, IntConsumer action) {
        float cy1 = Math.max(y1, top), cy2 = Math.min(y2, bottom);
        if (cy2 > cy1) hits.add(new Hit(x1, cy1, x2, cy2, action));
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        armed = confirmDelete;
        confirmDelete = null;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (mx >= h.x1 && mx <= h.x2 && my >= h.y1 && my <= h.y2) {
                closePicker();
                h.action.accept(button);
                return true;
            }
        }
        closePicker();
        commitEdit();
        return false;
    }

    /** Rule whose delete button was clicked once before this click (second click deletes). */
    private Rule armed;

    private void commitEdit() {
        if (editing != null) {
            String text = buffer.trim();
            if (!text.isEmpty()) module.replace(editing, editing.withText(text));
            editing = null;
        }
    }

    @Override public boolean mouseScrolled(double x, double y, double amount) {
        scroll = (float) Math.max(0, scroll - amount * 20);
        return true;
    }

    @Override public boolean keyPressed(String key) {
        if (editing != null) {
            if (Keys.ENTER.equals(key)) commitEdit();
            else if (Keys.ESCAPE.equals(key)) editing = null;
            else {
                var res = TextEdit.key(core.platform(), buffer, bufferAll, key);
                buffer = cut(res.text());
                bufferAll = res.allSelected();
            }
            return true;
        }
        if (Keys.ENTER.equals(key) || "key.keyboard.keypad.enter".equals(key)) {
            addInput();
            return true;
        }
        if (Keys.ESCAPE.equals(key) && (pickNew || pickFor != null)) {
            closePicker();
            return true;
        }
        var res = TextEdit.key(core.platform(), input, inputAll, key);
        if (!res.handled()) return false;
        input = cut(res.text());
        inputAll = res.allSelected();
        return true;
    }

    @Override public boolean charTyped(String text) {
        if (text.isEmpty() || text.codePointAt(0) < 32) return false;
        if (editing != null) {
            buffer = cut(TextEdit.type(buffer, bufferAll, text));
            bufferAll = false;
        } else {
            input = cut(TextEdit.type(input, inputAll, text));
            inputAll = false;
        }
        return true;
    }

    private static String cut(String s) {
        return s.length() > FriendsModule.MAX_TEXT ? s.substring(0, FriendsModule.MAX_TEXT) : s;
    }

    @Override public void onClose() { commitEdit(); }
}
