package dev.aio.core.modules.hud;

import dev.aio.core.hud.HudElement;
import dev.aio.core.platform.ItemRef;
import dev.aio.core.platform.Renderer;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.NumberSetting;
import dev.aio.core.util.Colors;

import java.util.List;

/**
 * Zeigt beim Wechseln des Items die Verzauberungen (und ähnliche Infos) über der Hotbar,
 * wie der Vanilla-Itemname, und blendet sie danach aus.
 */
public final class HeldItemInfoModule extends HudElement {
    private static final int LINE = 10;

    private final NumberSetting duration;
    private final BooleanSetting always;
    private final NumberSetting maxLines;

    private int lastSlot = -1;
    private String lastKey = "";
    private List<String> lines = List.of();
    private long shownAt;
    private int cachedW = -1;

    public HeldItemInfoModule() {
        super("helditeminfo", "Item-Info", "Verzauberungen des Items in der Hand über der Hotbar", 0.5, 0.80);
        duration = add(new NumberSetting("duration", "Anzeigedauer", 3, 1, 10, 0.5, " s"));
        always = add(new BooleanSetting("always", "Immer anzeigen", false));
        maxLines = add(new NumberSetting("maxLines", "Max. Zeilen", 6, 1, 12, 1));
    }

    @Override protected dev.aio.core.hud.ColorMode defaultColorMode() { return dev.aio.core.hud.ColorMode.STATIC; }

    @Override protected int defaultBackgroundColor() { return 0x00000000; }

    @Override public void update() {
        if (!platform().inWorld()) return;
        ItemRef held = platform().mainHand();
        int slot = platform().selectedSlot();
        // Details gehören zum Schlüssel, damit z. B. eine neue Verzauberung am Amboss auch erscheint
        List<String> next = held.isEmpty() ? List.of() : platform().heldItemDetails();
        String key = held.isEmpty() ? "" : held.id() + "|" + held.name() + "|" + next;
        if (slot != lastSlot || !key.equals(lastKey)) {
            lastSlot = slot;
            lastKey = key;
            int max = maxLines.intValue();
            if (next.size() > max) {
                List<String> cut = new java.util.ArrayList<>(next.subList(0, max - 1));
                cut.add("+" + (next.size() - max + 1) + " weitere");
                next = cut;
            }
            lines = next;
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

    @Override public boolean hasContent() { return !lines.isEmpty() && alpha() > 0.02f; }

    private List<String> shown(boolean preview) {
        return lines.isEmpty() && preview ? List.of("Schärfe V", "Plünderung III", "Haltbarkeit III") : lines;
    }

    @Override public int width(Renderer r) {
        if (cachedW < 0 || lines.isEmpty()) {
            int w = 0;
            for (String l : shown(true)) w = Math.max(w, r.textWidth(l));
            cachedW = w + PADDING * 2;
        }
        return cachedW;
    }

    @Override public int height(Renderer r) { return PADDING * 2 + shown(true).size() * LINE - 2; }

    @Override protected void renderContent(Renderer r, boolean preview) {
        float a = preview ? 1f : alpha();
        List<String> s = shown(preview);
        int w = width(r);
        for (int i = 0; i < s.size(); i++) {
            String line = s.get(i);
            float x = (w - r.textWidth(line)) / 2f;
            int color = Colors.fade(colorMode() == dev.aio.core.hud.ColorMode.STATIC ? 0xFFAAAAFF : dynamicColor(i * 12), a);
            if (Colors.alpha(color) < 8) continue;
            r.text(line, x, PADDING + i * LINE, color, shadow());
        }
    }
}
