package dev.aio.core.modules.inventory;

import dev.aio.core.event.Events;
import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.modules.inventory.data.InfoViews;
import dev.aio.core.modules.inventory.data.WorldInfo;
import dev.aio.core.platform.ItemRef;
import dev.aio.core.platform.RecipeView;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.NumberSetting;
import dev.aio.core.util.Log;

import java.util.ArrayList;
import java.util.List;

/** Item-Liste rechts in Inventaren mit Suche (@mod) und Rezeptansicht. */
public final class ItemListModule extends Module {
    private final NumberSetting maxColumns;
    private ItemPanel panel;

    private final BooleanSetting worldInfo;
    /** Welt-Infos (Beute, Handel, Spawns, Erze); null = noch nicht geladen. */
    private volatile WorldInfo info;
    private volatile boolean loading;

    public ItemListModule() {
        super("itemlist", "Item Browser", "All items next to your inventory (also creative), search with @mod, recipes (R/U)", Category.QOL, false);
        maxColumns = add(new NumberSetting("columns", "Max columns", 8, 3, 16, 1));
        worldInfo = add(new BooleanSetting("worldInfo", "Trades, loot, spawns and ores", true));
        // Neue Welt oder neuer Server: Daten (Datenpakete, Mods) können anders sein
        on(Events.ServerJoin.class, e -> info = null);
        on(Events.ServerLeave.class, e -> info = null);
    }

    /** Früher 9 Spalten: überdeckt bei GUI-Stufe 3 die Effekte, darum auf die neuen 8 umstellen. */
    @Override public void afterLoad() {
        if (maxColumns.intValue() == 9) maxColumns.reset();
    }

    /** Ob die Welt-Infos noch geladen werden. */
    public boolean worldInfoLoading() { return loading; }

    /** Startet das Laden im Hintergrund (einmal pro Welt). */
    public void ensureWorldInfo() {
        if (!worldInfo.on() || info != null || loading || !platform().inWorld()) return;
        loading = true;
        Thread t = new Thread(() -> {
            try {
                WorldInfo loaded = WorldInfo.load(platform());
                info = loaded;
                Log.info("Item Browser: world info loaded in " + loaded.loadMillis() + " ms");
            } catch (Throwable e) {
                Log.error("Item Browser: could not load world info", e);
            } finally {
                loading = false;
            }
        }, "AIO World Info");
        t.setDaemon(true);
        t.start();
    }

    /** Rezepte bzw. Verwendungen samt Handel, Beute, Berufen, Spawns, Erzen und Verzierungen. */
    public List<RecipeView> recipesFor(ItemRef item, boolean usages) {
        List<RecipeView> out = new ArrayList<>(platform().recipes(item, usages));
        out.addAll(platform().trimViews(item));
        ensureWorldInfo();
        WorldInfo wi = info;
        if (wi != null && worldInfo.on()) {
            InfoViews views = new InfoViews(platform(), wi);
            out.addAll(usages ? views.usages(item) : views.sources(item));
        }
        return out;
    }

    /** Ob gerade nachgeladen wird und die Ansicht später mehr zeigen kann. */
    public boolean worldInfoPending() { return worldInfo.on() && info == null; }


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
