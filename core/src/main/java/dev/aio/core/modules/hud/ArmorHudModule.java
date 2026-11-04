package dev.aio.core.modules.hud;

import dev.aio.core.hud.HudElement;
import dev.aio.core.platform.ItemRef;
import dev.aio.core.platform.Renderer;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.EnumSetting;
import dev.aio.core.util.Colors;

import java.util.ArrayList;
import java.util.List;

/** Rüstung und Items in der Hand mit Haltbarkeit als Balken, Zahl oder Prozent. */
public final class ArmorHudModule extends HudElement {
    public enum DurabilityMode implements EnumSetting.Named {
        BAR("Balken"), NUMBER("Zahl"), PERCENT("Prozent"), NONE("Aus");

        private final String displayName;

        DurabilityMode(String displayName) { this.displayName = displayName; }

        @Override public String displayName() { return displayName; }
    }

    public enum Orientation implements EnumSetting.Named {
        VERTICAL("Vertikal"), HORIZONTAL("Horizontal");

        private final String displayName;

        Orientation(String displayName) { this.displayName = displayName; }

        @Override public String displayName() { return displayName; }
    }

    /** {@code key} beschreibt den sichtbaren Zustand, damit nur bei echten Änderungen neu gemessen wird. */
    private record Entry(ItemRef item, String text, int color, String key) {}

    private static final int ICON = 16;
    private static final int ROW = 18;

    private final EnumSetting<DurabilityMode> durability;
    private final EnumSetting<Orientation> orientation;
    private final BooleanSetting showMainHand;
    private final BooleanSetting showOffHand;
    private final BooleanSetting colorByDurability;

    private List<Entry> entries = List.of();
    private int cachedW = -1;
    private int cachedH = -1;

    public ArmorHudModule() {
        super("armorhud", "ArmorHUD", "Rüstung, Haltbarkeit und Item in der Hand", 1.0, 0.5);
        durability = add(new EnumSetting<>("durability", "Haltbarkeit", DurabilityMode.PERCENT));
        orientation = add(new EnumSetting<>("orientation", "Ausrichtung", Orientation.VERTICAL));
        showMainHand = add(new BooleanSetting("mainHand", "Item in der Hand", true));
        showOffHand = add(new BooleanSetting("offHand", "Zweithand", true));
        colorByDurability = add(new BooleanSetting("colorByDurability", "Farbe nach Haltbarkeit", true));
        durability.onChange(v -> invalidate());
        orientation.onChange(v -> invalidate());
    }

    @Override protected boolean defaultBackground() { return false; }

    private void invalidate() {
        cachedW = -1;
        cachedH = -1;
    }

    @Override protected void onActivate() { update(); }

    @Override public void update() {
        List<Entry> next = new ArrayList<>(6);
        if (platform().inWorld()) {
            // Zweithand über der Rüstung, Haupthand darunter
            if (showOffHand.on()) addEntry(next, platform().offHand());
            for (ItemRef item : platform().armor()) addEntry(next, item);
            if (showMainHand.on()) addEntry(next, platform().mainHand());
        }
        if (!sameKeys(next, entries)) invalidate();
        entries = next; // Item-Handles immer aktualisieren, gemessen wird nur bei Änderung
    }

    private static boolean sameKeys(List<Entry> a, List<Entry> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) if (!a.get(i).key.equals(b.get(i).key)) return false;
        return true;
    }

    private void addEntry(List<Entry> list, ItemRef item) {
        if (item == null || item.isEmpty()) return;
        String text;
        int color = -1;
        if (item.isDamageable()) {
            text = switch (durability.get()) {
                case NUMBER -> String.valueOf(item.durability());
                case PERCENT -> Math.round(item.durabilityFraction() * 100) + "%";
                case BAR, NONE -> "";
            };
            if (colorByDurability.on()) color = Colors.health(item.durabilityFraction());
        } else {
            text = item.count() > 1 ? String.valueOf(item.count()) : "";
        }
        list.add(new Entry(item, text, color, item.name() + '|' + item.count() + '|' + item.damage() + '|' + text));
    }

    @Override public boolean hasContent() { return !entries.isEmpty(); }

    private int count(boolean preview) { return entries.isEmpty() && preview ? 4 : entries.size(); }

    @Override public int width(Renderer r) {
        if (cachedW < 0) measure(r);
        return cachedW;
    }

    @Override public int height(Renderer r) {
        if (cachedH < 0) measure(r);
        return cachedH;
    }

    private void measure(Renderer r) {
        int n = Math.max(1, count(true));
        int maxText = entries.isEmpty() ? r.textWidth("100") : 0;
        for (Entry e : entries) maxText = Math.max(maxText, r.textWidth(e.text));
        if (orientation.get() == Orientation.VERTICAL) {
            cachedW = PADDING * 2 + ICON + (maxText > 0 ? 2 + maxText : 0);
            cachedH = PADDING * 2 + n * ROW - 2;
        } else {
            int cell = Math.max(ICON, maxText) + 2;
            cachedW = PADDING * 2 + n * cell - 2;
            cachedH = PADDING * 2 + ICON + (maxText > 0 ? r.fontHeight() : 0);
        }
    }

    @Override protected void renderContent(Renderer r, boolean preview) {
        boolean placeholder = entries.isEmpty() && preview;
        int n = count(preview);
        boolean vertical = orientation.get() == Orientation.VERTICAL;
        int cell = vertical ? ROW : (width(r) - PADDING * 2 + 2) / Math.max(1, n);
        for (int i = 0; i < n; i++) {
            float x = vertical ? PADDING : PADDING + i * cell;
            float y = vertical ? PADDING + i * ROW : PADDING;
            if (placeholder) {
                r.fill(x, y, x + ICON, y + ICON, 0x40FFFFFF);
                if (durability.get() == DurabilityMode.NUMBER || durability.get() == DurabilityMode.PERCENT) {
                    drawLabel(r, "100", x, y, cell, vertical, colorByDurability.on() ? 0xFF55FF55 : -1);
                }
                continue;
            }
            Entry e = entries.get(i);
            float iconX = vertical ? x : x + (cell - 2 - ICON) / 2f;
            r.item(e.item, iconX, y, durability.get() == DurabilityMode.BAR);
            if (!e.text.isEmpty()) drawLabel(r, e.text, x, y, cell, vertical, e.color);
        }
    }

    /** {@code color} -1 = Farbmodus des Elements nutzen. */
    private void drawLabel(Renderer r, String text, float x, float y, int cell, boolean vertical, int color) {
        float tx = vertical ? x + ICON + 2 : x + (cell - 2) / 2f - r.textWidth(text) / 2f;
        float ty = vertical ? y + (ICON - r.fontHeight()) / 2f + 1 : y + ICON + 1;
        if (color == -1) drawText(r, text, tx, ty);
        else r.text(text, tx, ty, color, shadow());
    }
}
