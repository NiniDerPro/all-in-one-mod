package dev.aio.core.modules.hud;

import dev.aio.core.hud.ColorMode;
import dev.aio.core.hud.HudElement;
import dev.aio.core.i18n.Lang;
import dev.aio.core.platform.ItemRef;
import dev.aio.core.platform.Renderer;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.NumberSetting;
import dev.aio.core.util.Colors;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows the enchantments (and similar info) above the hotbar when switching the item,
 * like the vanilla item name, and fades them out afterwards.
 */
public final class HeldItemInfoModule extends HudElement {
    private static final int LINE = 10;
    /** Distance between item name and enchantments. */
    private static final int NAME_GAP = 2;
    /** Distance to the bottom screen edge: hotbar (22) plus air; in survival additionally above hearts/hunger. */
    private static final int MARGIN_CREATIVE = 27, MARGIN_SURVIVAL = 51;

    private final NumberSetting duration;
    private final BooleanSetting always;
    private final NumberSetting maxLines;
    private final BooleanSetting showName;

    private int lastSlot = -1;
    private String lastKey = "";
    private List<String> lines = List.of();
    private String itemName = "";
    private int itemNameColor = 0xFFFFFFFF;
    private long shownAt;
    private int cachedW = -1;

    public HeldItemInfoModule() {
        super("helditeminfo", "Held Item Info", "Name and enchantments of the held item above the hotbar", 0.5, 1.0);
        duration = add(new NumberSetting("duration", "Display time", 3, 1, 10, 0.5, " s"));
        always = add(new BooleanSetting("always", "Always show", false));
        maxLines = add(new NumberSetting("maxLines", "Max lines", 6, 1, 12, 1));
        showName = add(new BooleanSetting("showName", "Show item name", true));
        showName.onChange(v -> cachedW = -1);
    }

    @Override protected ColorMode defaultColorMode() { return ColorMode.STATIC; }

    @Override protected int defaultBackgroundColor() { return 0x00000000; }

    @Override protected boolean defaultBackground() { return false; }

    @Override protected int defaultTextColor() { return 0xFFAAAAAA; }

    /** Fixed default position above the hotbar: do not move automatically. */
    @Override public boolean isPlaced() { return true; }

    /** Switch the old default position (0,80) to the new one anchored at the bottom. */
    @Override public void afterLoad() {
        if (Math.abs(posY.get() - 0.80) < 1e-6) posY.reset();
        if (textColor.argb() == 0xFFFFFFFF) textColor.reset(); // used to be fixed blue, now grey like vanilla
    }

    /** Anchored at the bottom: 1.0 = directly above the hotbar (or above hearts and hunger in survival). */
    private float bottomMargin() {
        return core != null && platform().foodState().visible() ? MARGIN_SURVIVAL : MARGIN_CREATIVE;
    }

    @Override public float screenY(Renderer r) {
        float free = Math.max(0, r.screenHeight() - bottomMargin() - scaledHeight(r));
        return (float) (posY.get() * free);
    }

    @Override public void setScreenPos(Renderer r, float x, float y) {
        super.setScreenPos(r, x, y);
        float free = r.screenHeight() - bottomMargin() - scaledHeight(r);
        posY.set(free <= 0 ? 0.0 : Math.max(0.0, Math.min(1.0, y / (double) free)));
    }

    @Override public void update() {
        if (!platform().inWorld()) return;
        ItemRef held = platform().mainHand();
        int slot = platform().selectedSlot();
        // Details belong to the key so that e.g. a new enchantment at the anvil also appears
        List<String> next = held.isEmpty() ? List.of() : platform().heldItemDetails();
        String key = held.isEmpty() ? "" : held.id() + "|" + held.name() + "|" + next;
        if (slot != lastSlot || !key.equals(lastKey)) {
            lastSlot = slot;
            lastKey = key;
            int max = maxLines.intValue();
            if (next.size() > max) {
                List<String> cut = new ArrayList<>(next.subList(0, max - 1));
                cut.add("+" + (next.size() - max + 1) + " more");
                next = cut;
            }
            lines = next;
            itemName = held.isEmpty() ? "" : held.name();
            itemNameColor = held.isEmpty() ? 0xFFFFFFFF : held.nameColor();
            cachedW = -1;
            shownAt = System.currentTimeMillis();
        }
    }

    private float alpha() {
        if (always.on()) return 1f;
        long elapsed = System.currentTimeMillis() - shownAt;
        long total = (long) (duration.get() * 1000);
        if (elapsed >= total) return 0f;
        long fadeStart = total - 500;
        return elapsed < fadeStart ? 1f : 1f - (elapsed - fadeStart) / 500f;
    }

    /** Whether the item name is above the enchantments (then the adapter hides the vanilla name). */
    public boolean replacesVanillaName() { return isActive() && showName.on(); }

    @Override public boolean hasContent() { return (!lines.isEmpty() || !name(false).isEmpty()) && alpha() > 0.02f; }

    private boolean previewing(boolean preview) { return lines.isEmpty() && preview; }

    private List<String> shown(boolean preview) {
        return previewing(preview) ? List.of("Sharpness V", "Looting III", "Unbreaking III") : lines;
    }

    private String name(boolean preview) {
        if (!showName.on()) return "";
        return previewing(preview) ? Lang.tr("aio.ui.held_item_info.diamond_sword", "Diamond Sword") : itemName;
    }

    private int nameHeight(boolean preview) { return name(preview).isEmpty() ? 0 : LINE + NAME_GAP; }

    @Override public int width(Renderer r) {
        if (cachedW < 0 || lines.isEmpty()) {
            int w = r.textWidth(name(true));
            for (String l : shown(true)) w = Math.max(w, r.textWidth(l));
            cachedW = w + PADDING * 2;
        }
        return cachedW;
    }

    @Override public int height(Renderer r) { return PADDING * 2 + nameHeight(true) + shown(true).size() * LINE - 2; }

    @Override protected void renderContent(Renderer r, boolean preview) {
        float a = preview ? 1f : alpha();
        List<String> s = shown(preview);
        int w = width(r);
        String name = name(preview);
        if (!name.isEmpty()) {
            int c = Colors.fade(previewing(preview) ? 0xFF55FFFF : itemNameColor, a);
            if (Colors.alpha(c) >= 8) r.text(name, (w - r.textWidth(name)) / 2f, PADDING, c, shadow());
        }
        float top = PADDING + nameHeight(preview);
        for (int i = 0; i < s.size(); i++) {
            String line = s.get(i);
            float x = (w - r.textWidth(line)) / 2f;
            // Grey like the enchantments in the vanilla tooltip (curses bring their red themselves)
            int color = Colors.fade(colorMode() == ColorMode.STATIC ? textColor() : dynamicColor(i * 12), a);
            if (Colors.alpha(color) < 8) continue;
            r.text(line, x, top + i * LINE, color, shadow());
        }
    }
}
