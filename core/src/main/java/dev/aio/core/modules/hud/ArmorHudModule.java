package dev.aio.core.modules.hud;

import dev.aio.core.i18n.Lang;

import dev.aio.core.hud.HudElement;
import dev.aio.core.hud.HudPart;
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
        BAR("Bar"), NUMBER("Number"), PERCENT("Percent"), NONE("Off");

        private final String displayName;

        DurabilityMode(String displayName) { this.displayName = displayName; }

        @Override public String displayName() { return displayName; }
    }

    public enum Orientation implements EnumSetting.Named {
        VERTICAL("Vertical"), HORIZONTAL("Horizontal");

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
    /** Pro Platz (Zweithand, Kopf, Brust, Beine, Füße, Haupthand) der Eintrag oder null. */
    private Entry[] slots = new Entry[6];
    private final HudPart[] parts = new HudPart[6];
    private static final String[][] PARTS = {{"offhand", "Offhand"}, {"head", "Helmet"}, {"chest", "Chestplate"},
            {"legs", Lang.tr("aio.ui.armor_hud.leggings", "Leggings")}, {"feet", Lang.tr("aio.ui.armor_hud.boots", "Boots")}, {"mainhand", Lang.tr("aio.ui.armor_hud.main_hand", "Main hand")}};
    /** Innenabstand eines einzelnen Teils: so passen die Teile lückenlos untereinander. */
    private static final int PART_PAD = 1;
    private final BooleanSetting edgeMigrated;
    private int cachedW = -1;
    private int cachedH = -1;

    public ArmorHudModule() {
        super("armorhud", "Armor HUD", "Armor, durability and held items", 0.0, 0.5);
        edgeMigrated = add(new BooleanSetting("edgeMigrated", "Edge migrated", false));
        edgeMigrated.hidden();
        durability = add(new EnumSetting<>("durability", "Durability", DurabilityMode.PERCENT));
        orientation = add(new EnumSetting<>("orientation", "Orientation", Orientation.VERTICAL));
        showMainHand = add(new BooleanSetting("mainHand", "Main hand", true));
        showOffHand = add(new BooleanSetting("offHand", "Offhand", false));
        colorByDurability = add(new BooleanSetting("colorByDurability", "Color by durability", true));
        for (int i = 0; i < PARTS.length; i++) parts[i] = addPart(PARTS[i][0], PARTS[i][1]);
        durability.onChange(v -> invalidate());
        orientation.onChange(v -> invalidate());
    }

    @Override protected boolean defaultBackground() { return false; }

    /** Ganz an den Rand: ohne Hintergrund liegen die Icons direkt an, mit Hintergrund der Kasten. */
    @Override protected float edgeMargin() { return background.on() ? 0 : -(PADDING - 1) * scale.get().floatValue(); }

    @Override protected float partEdgeMargin() { return background.on() ? 0 : -PART_PAD; }

    /** Früher hielt alles 3 px Abstand zum Rand; was dort stand, kommt einmalig ganz an den Rand. */
    @Override public void afterLoad() {
        super.afterLoad();
        if (edgeMigrated.on()) return;
        if (posX.get() <= 0.03) posX.set(0.0);
        for (HudPart p : parts) if (p != null) p.snapLeft(0.03);
        edgeMigrated.set(true);
    }

    /** Neue Standards (links, ohne Zweithand) einmalig auch für bestehende Configs. */
    @Override public void applyNewDefaults() {
        posX.reset();
        posY.reset();
        showOffHand.reset();
    }

    private void invalidate() {
        cachedW = -1;
        cachedH = -1;
    }

    @Override protected void onActivate() { update(); }

    @Override public void update() {
        List<Entry> next = new ArrayList<>(6);
        Entry[] nextSlots = new Entry[6];
        if (platform().inWorld()) {
            // Zweithand über der Rüstung, Haupthand darunter
            List<ItemRef> armor = platform().armor();
            for (int i = 0; i < 6; i++) {
                if (i == 0 && !showOffHand.on() || i == 5 && !showMainHand.on()) continue;
                ItemRef item = i == 0 ? platform().offHand() : i == 5 ? platform().mainHand() : (i - 1 < armor.size() ? armor.get(i - 1) : null);
                int before = next.size();
                addEntry(next, item);
                if (next.size() > before) nextSlots[i] = next.get(before);
            }
        }
        slots = nextSlots;
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
                float iconX = vertical ? x : x + (cell - 2 - ICON) / 2f;
                drawSample(r, i + 1, iconX, y);
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

    /** Beispiel-Ausrüstung für die Vorschau im Editor, solange der Spieler nichts trägt. */
    private static final String[] SAMPLE = {"minecraft:shield", "minecraft:diamond_helmet", "minecraft:diamond_chestplate",
            "minecraft:diamond_leggings", "minecraft:diamond_boots", "minecraft:diamond_sword"};
    private final ItemRef[] sampleItems = new ItemRef[SAMPLE.length];

    private void drawSample(Renderer r, int index, float x, float y) {
        int i = Math.max(0, Math.min(SAMPLE.length - 1, index));
        if (sampleItems[i] == null) sampleItems[i] = platform().item(SAMPLE[i]);
        if (sampleItems[i].isEmpty()) r.fill(x, y, x + ICON, y + ICON, 0x40FFFFFF);
        else r.item(sampleItems[i], x, y, false);
    }

    /** {@code color} -1 = Farbmodus des Elements nutzen. */
    private void drawLabel(Renderer r, String text, float x, float y, int cell, boolean vertical, int color) {
        float tx = vertical ? x + ICON + 2 : x + (cell - 2) / 2f - r.textWidth(text) / 2f;
        float ty = vertical ? y + (ICON - r.fontHeight()) / 2f + 1 : y + ICON + 1;
        if (color == -1) drawText(r, text, tx, ty);
        else r.text(text, tx, ty, color, shadow());
    }

    // ---- Einzelne Teile ----

    private int indexOf(HudPart part) {
        for (int i = 0; i < parts.length; i++) if (parts[i] == part) return i;
        return 0;
    }

    @Override protected boolean partEnabled(HudPart part) {
        int i = indexOf(part);
        return i == 0 ? showOffHand.on() : i != 5 || showMainHand.on();
    }

    @Override protected boolean partHasContent(HudPart part) { return slots[indexOf(part)] != null; }

    private String partText(Renderer r, HudPart part) {
        Entry e = slots[indexOf(part)];
        if (e != null) return e.text;
        return durability.get() == DurabilityMode.NUMBER || durability.get() == DurabilityMode.PERCENT ? "100" : "";
    }

    @Override protected int partWidth(Renderer r, HudPart part) {
        int tw = r.textWidth(partText(r, part));
        if (orientation.get() == Orientation.VERTICAL) return PART_PAD * 2 + ICON + (tw > 0 ? 2 + tw : 0);
        return PART_PAD * 2 + Math.max(ICON, tw);
    }

    @Override protected int partHeight(Renderer r, HudPart part) {
        if (orientation.get() == Orientation.VERTICAL) return PART_PAD * 2 + ICON;
        return PART_PAD * 2 + ICON + (partText(r, part).isEmpty() ? 0 : r.fontHeight());
    }

    @Override protected float[] partOffset(Renderer r, HudPart part) {
        // Reihenfolge wie im Block, gezählt über die eingeschalteten Plätze
        int n = 0;
        for (int i = 0; i < indexOf(part); i++) if (partEnabled(parts[i])) n++;
        if (orientation.get() == Orientation.VERTICAL) return new float[]{PADDING - PART_PAD, PADDING - PART_PAD + n * ROW};
        return new float[]{PADDING - PART_PAD + n * (ICON + 4), PADDING - PART_PAD};
    }

    @Override protected void renderPart(Renderer r, HudPart part, boolean preview) {
        Entry e = slots[indexOf(part)];
        boolean vertical = orientation.get() == Orientation.VERTICAL;
        int w = partWidth(r, part);
        float iconX = vertical ? PART_PAD : (w - ICON) / 2f;
        String text = partText(r, part);
        if (e == null) drawSample(r, indexOf(part), iconX, PART_PAD);
        else r.item(e.item, iconX, PART_PAD, durability.get() == DurabilityMode.BAR);
        if (text.isEmpty()) return;
        int color = e != null ? e.color : colorByDurability.on() ? 0xFF55FF55 : -1;
        float tx = vertical ? PART_PAD + ICON + 2 : w / 2f - r.textWidth(text) / 2f;
        float ty = vertical ? PART_PAD + (ICON - r.fontHeight()) / 2f + 1 : PART_PAD + ICON + 1;
        if (color == -1) drawText(r, text, tx, ty);
        else r.text(text, tx, ty, color, shadow());
    }
}
