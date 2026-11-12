package dev.aio.core.modules.hud;

import dev.aio.core.hud.HudElement;
import dev.aio.core.platform.ItemRef;
import dev.aio.core.platform.Renderer;
import dev.aio.core.setting.BooleanSetting;

/** Counts an item in the inventory and shows it with an icon, e.g. totems or arrows. */
public class ItemCounterModule extends HudElement {
    private final String query;
    private final String iconId;
    private final BooleanSetting hideWhenZero;
    private final BooleanSetting warnWhenZero;
    private int count;
    private ItemRef icon = ItemRef.EMPTY;

    protected ItemCounterModule(String id, String name, String description, String query, String iconId, double x, double y) {
        super(id, name, description, x, y);
        this.query = query;
        this.iconId = iconId;
        hideWhenZero = add(new BooleanSetting("hideWhenZero", "Hide at 0", false));
        warnWhenZero = add(new BooleanSetting("warnWhenZero", "Red at 0", true));
    }

    @Override protected void onActivate() { update(); }

    @Override public void update() {
        if (!platform().inWorld()) {
            count = 0;
            return;
        }
        count = platform().countItems(query);
        if (icon.isEmpty()) icon = platform().item(iconId);
    }

    public int count() { return count; }

    @Override public boolean hasContent() { return platform().inWorld() && !(hideWhenZero.on() && count == 0); }

    @Override public int width(Renderer r) { return PADDING * 2 + 16 + 3 + r.textWidth(String.valueOf(Math.max(count, 99))); }

    @Override public int height(Renderer r) { return PADDING * 2 + 16 - 2; }

    @Override protected void renderContent(Renderer r, boolean preview) {
        if (!icon.isEmpty()) r.item(icon, PADDING, PADDING - 1, false);
        else r.fill(PADDING, PADDING - 1, PADDING + 16, PADDING + 15, 0x40FFFFFF);
        String text = String.valueOf(count);
        float ty = PADDING + (16 - r.fontHeight()) / 2f;
        if (count == 0 && warnWhenZero.on()) r.text(text, PADDING + 19, ty, 0xFFFF5555, shadow());
        else drawText(r, text, PADDING + 19, ty);
    }
}
