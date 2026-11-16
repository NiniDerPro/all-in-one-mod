package dev.aio.core.modules.inventory;

import dev.aio.core.AioCore;
import dev.aio.core.i18n.Lang;
import dev.aio.core.platform.ContainerView;
import dev.aio.core.platform.ContainerView.Click;
import dev.aio.core.platform.ItemRef;
import dev.aio.core.platform.Keys;
import dev.aio.core.platform.RecipeKind;
import dev.aio.core.platform.RecipeView;

/**
 * One-click crafting (middle mouse button): puts a recipe into the open crafting grid and takes the
 * result straight into the inventory. Shift crafts as many as possible.
 */
public final class QuickCraft {
    /** Result slot of the 2×2 inventory grid and the crafting table. */
    static final int RESULT_SLOT = 0;

    private QuickCraft() {}

    /** Crafts the first recipe for {@code item} that works with the open grid and the inventory. */
    public static boolean craftItem(AioCore core, ItemRef item) {
        ContainerView c = core.platform().container();
        if (c == null || c.grid() == null || !c.grid().kinds().contains(RecipeKind.CRAFTING)) {
            core.platform().notify(Lang.tr("aio.ui.quickcraft.no_grid", "Open your inventory or a crafting table to craft"));
            return false;
        }
        RecipeView fallback = null;
        for (RecipeView rec : core.platform().recipes(item, false)) {
            if (rec.kind() != RecipeKind.CRAFTING) continue;
            RecipeTransfer.Status st = RecipeTransfer.check(c, rec);
            if (st == RecipeTransfer.Status.OK) return craft(core, rec);
            if (fallback == null) fallback = rec;
        }
        core.platform().notify(fallback == null
                ? Lang.tr("aio.ui.quickcraft.no_recipe", "No crafting recipe for this item")
                : Lang.tr("aio.ui.quickcraft.missing", "Missing ingredients (or too big for 2×2)"));
        return false;
    }

    /** Crafts exactly this recipe; returns whether anything was crafted. */
    public static boolean craft(AioCore core, RecipeView rec) {
        ContainerView c = core.platform().container();
        if (c == null || rec.kind() != RecipeKind.CRAFTING) return false;
        boolean all = core.platform().isKeyDown(Keys.LEFT_SHIFT) || core.platform().isKeyDown(Keys.RIGHT_SHIFT);
        if (RecipeTransfer.transfer(c, rec, all) <= 0) {
            core.platform().notify(Lang.tr("aio.ui.quickcraft.missing", "Missing ingredients (or too big for 2×2)"));
            return false;
        }
        // Shift-click on the result: the server crafts as often as the grid allows and moves it into the inventory
        c.click(RESULT_SLOT, 0, Click.QUICK_MOVE);
        return true;
    }
}
