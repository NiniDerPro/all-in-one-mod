package dev.aio.core.modules.hud;

import dev.aio.core.hud.ColorMode;
import dev.aio.core.hud.HudElement;
import dev.aio.core.i18n.Lang;
import dev.aio.core.platform.GameKey;
import dev.aio.core.platform.Renderer;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.ColorSetting;
import dev.aio.core.ui.Draw;
import dev.aio.core.setting.EnumSetting;
import dev.aio.core.setting.NumberSetting;
import dev.aio.core.util.Animation;
import dev.aio.core.util.Colors;

import java.util.ArrayList;
import java.util.List;

/** WASD, Leertaste und Maustasten mit sanftem Farbwechsel beim Drücken. */
public final class KeystrokesModule extends HudElement {
    public enum Layout implements EnumSetting.Named {
        WASD("WASD"), WASD_MOUSE("WASD + Mouse"), FULL("WASD + Mouse + Space");

        private final String displayName;

        Layout(String displayName) { this.displayName = displayName; }

        @Override public String displayName() { return displayName; }
    }

    /** Eine Taste im Layout, Koordinaten in Pixeln relativ zur oberen linken Ecke. */
    private static final class KeyBox {
        final GameKey key;
        final float x, y, w, h;
        final Animation anim = new Animation();
        String label = "";

        KeyBox(GameKey key, float x, float y, float w, float h) {
            this.key = key;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }

    private final EnumSetting<Layout> layout;
    private final NumberSetting keySize;
    private final NumberSetting gap;
    private final NumberSetting fadeMs;
    private final ColorSetting pressedColor;
    private final ColorSetting pressedTextColor;
    private final BooleanSetting showCps;
    private final BooleanSetting rainbowPressed;

    private List<KeyBox> boxes = List.of();
    private int totalW, totalH;
    private boolean layoutDirty = true;

    public KeystrokesModule() {
        super("keystrokes", "Keystrokes", "Shows pressed movement keys and mouse buttons", 0.0, 0.3);
        layout = add(new EnumSetting<>("layout", "Layout", Layout.FULL));
        keySize = add(new NumberSetting("keySize", "Key size", 22, 14, 40, 1, " px"));
        gap = add(new NumberSetting("gap", "Spacing", 2, 0, 6, 1, " px"));
        fadeMs = add(new NumberSetting("fade", "Fade", 120, 0, 500, 10, " ms"));
        pressedColor = add(new ColorSetting("pressedColor", "Pressed color", 0xC0FFFFFF));
        pressedTextColor = add(new ColorSetting("pressedTextColor", "Pressed text color", 0xFF000000));
        showCps = add(new BooleanSetting("showCps", "CPS in mouse buttons", true));
        rainbowPressed = add(new BooleanSetting("rainbowPressed", "Color mode when pressed", false));
        showCps.visibleWhen(() -> layout.get() != Layout.WASD);
        layout.onChange(v -> layoutDirty = true);
        keySize.onChange(v -> layoutDirty = true);
        gap.onChange(v -> layoutDirty = true);
    }

    @Override protected void onActivate() {
        core.clicks().acquire();
        layoutDirty = true;
    }

    @Override protected void onDeactivate() {
        core.clicks().release();
    }

    private void rebuildLayout() {
        float k = keySize.floatValue();
        float g = gap.floatValue();
        List<KeyBox> list = new ArrayList<>();
        list.add(new KeyBox(GameKey.FORWARD, k + g, 0, k, k));
        list.add(new KeyBox(GameKey.LEFT, 0, k + g, k, k));
        list.add(new KeyBox(GameKey.BACK, k + g, k + g, k, k));
        list.add(new KeyBox(GameKey.RIGHT, 2 * (k + g), k + g, k, k));
        float y = 2 * (k + g);
        float fullW = 3 * k + 2 * g;
        if (layout.get() != Layout.WASD) {
            float half = (fullW - g) / 2f;
            list.add(new KeyBox(GameKey.ATTACK, 0, y, half, k));
            list.add(new KeyBox(GameKey.USE, half + g, y, half, k));
            y += k + g;
        }
        if (layout.get() == Layout.FULL) {
            list.add(new KeyBox(GameKey.JUMP, 0, y, fullW, Math.max(8, k * 0.55f)));
            y += Math.max(8, k * 0.55f) + g;
        }
        boxes = list;
        totalW = Math.round(fullW);
        totalH = Math.round(y - g);
        layoutDirty = false;
        refreshLabels();
    }

    private void refreshLabels() {
        for (KeyBox b : boxes) {
            b.label = switch (b.key) {
                case ATTACK -> "LMB";
                case USE -> "RMB";
                case JUMP -> "———";
                default -> platform().gameKeyName(b.key);
            };
        }
    }

    @Override public void update() {
        if (layoutDirty) rebuildLayout();
        else refreshLabels();
    }

    @Override public int width(Renderer r) {
        if (layoutDirty) rebuildLayout();
        return totalW;
    }

    @Override public int height(Renderer r) {
        if (layoutDirty) rebuildLayout();
        return totalH;
    }

    @Override protected boolean drawsDefaultBackground() { return false; }

    @Override protected void renderContent(Renderer r, boolean preview) {
        if (layoutDirty) rebuildLayout();
        long now = System.nanoTime();
        float fade = fadeMs.floatValue();
        int idle = background.on() ? backgroundColor.argb() : 0;
        int fontH = r.fontHeight();
        for (KeyBox b : boxes) {
            boolean down = platform().isGameKeyDown(b.key);
            float t = b.anim.update(down ? 1f : 0f, fade, now);
            int pressed = rainbowPressed.on()
                    ? Colors.withAlpha(dynamicColor(b.x + b.y), Colors.alpha(pressedColor.argb()))
                    : pressedColor.argb();
            int bg = Colors.lerp(idle, pressed, t);
            if (Colors.alpha(bg) > 0) Draw.round(r, b.x, b.y, b.x + b.w, b.y + b.h, 2, bg);
            int baseText = colorMode() == ColorMode.STATIC || colorMode() == ColorMode.CLEAN ? textColor() : dynamicColor(b.x + b.y + 40);
            int fg = Colors.lerp(baseText, pressedTextColor.argb(), t);

            if ((b.key == GameKey.ATTACK || b.key == GameKey.USE) && showCps.on()) {
                int cps = b.key == GameKey.ATTACK ? core.clicks().leftCps() : core.clicks().rightCps();
                float total = fontH * 2 - 2;
                float top = b.y + (b.h - total) / 2f;
                r.centeredText(b.label, b.x + b.w / 2f, top, fg, shadow() && t < 0.5f);
                r.push();
                r.translate(b.x + b.w / 2f, top + fontH);
                r.scale(0.75f);
                r.centeredText(Lang.tr("aio.ui.keystrokes.cps", "%s CPS", cps), 0, 0, fg, shadow() && t < 0.5f);
                r.pop();
            } else if (b.key == GameKey.JUMP) {
                float lineW = b.w * 0.45f;
                float cy = b.y + b.h / 2f;
                r.fill(b.x + (b.w - lineW) / 2f, cy - 0.5f, b.x + (b.w + lineW) / 2f, cy + 0.5f, fg);
            } else {
                r.centeredText(b.label, b.x + b.w / 2f, b.y + (b.h - fontH + 2) / 2f, fg, shadow() && t < 0.5f);
            }
        }
    }
}
