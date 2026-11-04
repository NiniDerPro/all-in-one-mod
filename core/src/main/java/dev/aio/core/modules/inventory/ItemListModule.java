package dev.aio.core.modules.inventory;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.platform.ItemRef;
import dev.aio.core.setting.NumberSetting;

/** Item-Liste rechts in Inventaren mit Suche (@mod) und Rezeptansicht. */
public final class ItemListModule extends Module {
    private final NumberSetting maxColumns;
    private ItemPanel panel;

    private final dev.aio.core.setting.BooleanSetting worldInfo;
    /** Welt-Infos (Beute, Handel, Spawns, Erze); null = noch nicht geladen. */
    private volatile dev.aio.core.modules.inventory.data.WorldInfo info;
    private volatile boolean loading;

    public ItemListModule() {
        super("itemlist", "Item-Browser", "Alle Items rechts im Inventar (auch Kreativ), Suche mit @mod, Rezepte (R/U)", Category.INVENTORY, false);
        maxColumns = add(new NumberSetting("columns", "Max. Spalten", 9, 3, 16, 1));
        worldInfo = add(new dev.aio.core.setting.BooleanSetting("worldInfo", "Handel, Beute, Spawns & Erze", true));
        // Neue Welt oder neuer Server: Daten (Datenpakete, Mods) können anders sein
        on(dev.aio.core.event.Events.ServerJoin.class, e -> info = null);
        on(dev.aio.core.event.Events.ServerLeave.class, e -> info = null);
    }

    /** Ob die Welt-Infos noch geladen werden. */
    public boolean worldInfoLoading() { return loading; }

    /** Startet das Laden im Hintergrund (einmal pro Welt). */
    public void ensureWorldInfo() {
        if (!worldInfo.on() || info != null || loading || !platform().inWorld()) return;
        loading = true;
        Thread t = new Thread(() -> {
            try {
                dev.aio.core.modules.inventory.data.WorldInfo loaded = dev.aio.core.modules.inventory.data.WorldInfo.load(platform());
                info = loaded;
                dev.aio.core.util.Log.info("Item-Browser: Welt-Infos in " + loaded.loadMillis() + " ms geladen");
            } catch (Throwable e) {
                dev.aio.core.util.Log.error("Item-Browser: Welt-Infos konnten nicht geladen werden", e);
            } finally {
                loading = false;
            }
        }, "AIO Welt-Infos");
        t.setDaemon(true);
        t.start();
    }

    /** Rezepte bzw. Verwendungen samt Handel, Beute, Berufen, Spawns, Erzen und Verzierungen. */
    public java.util.List<dev.aio.core.platform.RecipeView> recipesFor(ItemRef item, boolean usages) {
        java.util.List<dev.aio.core.platform.RecipeView> out = new java.util.ArrayList<>(platform().recipes(item, usages));
        out.addAll(platform().trimViews(item));
        ensureWorldInfo();
        dev.aio.core.modules.inventory.data.WorldInfo wi = info;
        if (wi != null && worldInfo.on()) {
            dev.aio.core.modules.inventory.data.InfoViews views = new dev.aio.core.modules.inventory.data.InfoViews(platform(), wi);
            out.addAll(usages ? views.usages(item) : views.sources(item));
        }
        return out;
    }

    /** Ob gerade nachgeladen wird und die Ansicht später mehr zeigen kann. */
    public boolean worldInfoPending() { return worldInfo.on() && info == null; }

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
