package dev.aio.core.modules.inventory;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.platform.ItemRef;
import dev.aio.core.setting.NumberSetting;

/** Item-Liste rechts in Inventaren mit Suche (@mod) und Rezeptansicht. */
public final class ItemListModule extends Module {
    private final NumberSetting maxColumns;
    private ItemPanel panel;

    public ItemListModule() {
        super("itemlist", "Item-Browser", "Alle Items rechts im Inventar (auch Kreativ), Suche mit @mod, Rezepte (R/U)", Category.INVENTORY, false);
        maxColumns = add(new NumberSetting("columns", "Max. Spalten", 9, 3, 16, 1));
    }

    @Override public boolean enabledByDefault() { return true; }

    int maxColumns() { return maxColumns.intValue(); }

    /** Panel für Inventar-Screens, null wenn das Modul aus ist. */
    /** Mods, die selbst eine Item-Liste mitbringen: dann keine zweite einblenden. */
    private static final String[] CONFLICTS = {"jei", "roughlyenoughitems", "emi"};

    public ItemPanel panel() {
        if (!isActive() || conflict()) return null;
        if (panel == null) panel = new ItemPanel(core, this);
        return panel;
    }

    public boolean conflict() {
        for (String id : CONFLICTS) if (platform().isModLoaded(id)) return true;
        return false;
    }

    public void showRecipes(ItemRef item, boolean usages) {
        if (item == null || item.isEmpty()) return;
        platform().openScreen(new RecipeScreen(core, item, usages));
    }
}
