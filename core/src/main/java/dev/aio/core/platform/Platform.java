package dev.aio.core.platform;

import dev.aio.core.ui.UiScreen;

import com.google.gson.JsonElement;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Everything the core needs to know about the game or change in it.
 *
 * <p>This is the version abstraction layer: for a new MC version only this
 * interface (plus {@link Renderer}, {@link ItemRef} and a handful of mixins) is implemented anew.
 */
public interface Platform {
    // ---- Zustand ----
    boolean inWorld();

    boolean isSingleplayer();

    /** Server address (e.g. {@code play.example.net}) or null in singleplayer/menu. */
    String serverAddress();

    int fps();

    int ping();

    Vec3 playerPos();

    float playerYaw();

    float playerPitch();

    String biome();

    /** Armor from head to feet, always 4 entries. */
    List<ItemRef> armor();

    ItemRef mainHand();

    ItemRef offHand();

    List<EffectInfo> effects();

    /**
     * Block or entity the player is looking at, null if nothing.
     *
     * @param maxDistance 0 = like the vanilla crosshair (reach), otherwise farther (blocks only)
     */
    LookTarget lookTarget(double maxDistance);

    /** Whether the crosshair is currently on a living entity (for Auto Sword). */
    boolean crosshairOnLiving();

    /** Whether another mod is loaded (e.g. "jei"), to switch off duplicate features. */
    boolean isModLoaded(String modId);

    /** Whether the camera hook for freelook applies in this MC version. Otherwise freelook must not intercept the mouse. */
    boolean freelookSupported();

    // ---- Welt ----

    /** Unique key of the current world (server address or singleplayer world name). */
    String worldKey();

    String playerName();

    /** Time of the overworld clock in ticks (day = 24000). */
    long dayTime();

    /** "overworld", "the_nether", "the_end" or the id of another dimension. */
    String dimension();

    /**
     * The actual camera for screen projections (waypoint markers): interpolated,
     * with third-person distance, freelook direction and the real field of view (sprinting, zoom, ...).
     */
    CameraView camera(float partialTick);

    boolean isDead();

    /** Play time according to the statistics in ticks, -1 if unknown. */
    long playTimeTicks();

    /** Requests current statistics from the server (like the statistics menu). */
    void requestStats();

    ServerInfo serverInfo();

    /** Counts items in the inventory: registry id ({@code minecraft:arrow}) or tag ({@code #minecraft:arrows}). */
    int countItems(String idOrTag);

    /** Item icon for a registry id. */
    ItemRef item(String id);

    /** Selected item in the hotbar changed, returns: info lines (enchantments or similar). */
    List<String> heldItemDetails();

    // ---- Hotbar ----

    int selectedSlot();

    void selectSlot(int slot);

    /** Best hotbar slot for breaking the viewed block, -1 if none is better. */
    int bestToolSlot();

    /** Best hotbar slot for fighting, -1 if there is no weapon. */
    int bestWeaponSlot(boolean preferAxe);

    // ---- Inventar / Item-Liste ----

    /** The currently open inventory or null. */
    ContainerView container();

    /** All items from the registry (for the item list). */
    List<ItemRef> allItems();

    /** Recipes that make {@code item} ({@code usages = true}: those that use it). May be empty. */
    List<RecipeView> recipes(ItemRef item, boolean usages);

    /** Whether recipes are available (singleplayer or synced from the server). */
    boolean recipesAvailable();

    // ---- Optionen / Sonstiges ----

    /** Value of a vanilla option or {@code NaN} while the options are not loaded yet (game start). */
    double option(VanillaOption option);

    void setOption(VanillaOption option, double value);

    /** Kurzer Hinweiston (z. B. Namens-Alarm). */
    void playAlertSound();

    /** Kurzer UI-Ton: "pling", "bell", "anvil", "xp", "bass", "break". */
    default void playSound(String id, float volume, float pitch) { playAlertSound(); }

    // ---- Eingabe ----
    boolean isGameKeyDown(GameKey key);

    /** Display name of the current binding of a vanilla key, e.g. "W" or "LMB". */
    String gameKeyName(GameKey key);

    /** Checks whether a key or mouse button (MC key name, see {@link Keys}) is currently held. */
    boolean isKeyDown(String key);

    /**
     * Installed mods that have the same feature as an AIO module: module id → name of the mod.
     * These mods take priority, the AIO module then stays off.
     */
    default Map<String, String> conflictingMods() { return Map.of(); }

    /**
     * Modules whose hook into the game does not apply (mostly because another mod changes the same place):
     * module id → short description. Queried after the world is loaded.
     */
    default Map<String, String> brokenHooks() { return Map.of(); }

    /** Entities for the Health Indicator (interpolated, without the own player). */
    default List<HealthTarget> healthTargets(double maxDistance, float partialTick, boolean players,
                                                       boolean hostile, boolean passive, boolean throughWalls) {
        return List.of();
    }

    /** Nametags for players, mobs, items on the ground and TNT (without the own player). */
    default List<NametagTarget> nametagTargets(double maxDistance, float partialTick, boolean players, boolean mobs,
                                                         boolean items, boolean tnt, boolean throughWalls) {
        return List.of();
    }

    /** Whether the F3 screen is currently open. */
    default boolean debugScreenOpen() { return false; }

    /** Attack charge of the own player, 0..1 (1 = full hit possible). */
    default float attackCharge() { return 1f; }

    /** Real screen pixels per GUI pixel. */
    default double guiScale() { return 1; }

    /**
     * Draws the profiler pie chart (F3+1) into the local rectangle 0..w × 0..h, flush at the bottom.
     * @return false if there is (not yet) any data
     */
    default boolean drawProfilerChart(Renderer r, int w, int h) { return false; }

    /** Level in the profiler chart that is selected in F3+1 with the number keys (e.g. "root.gameRenderer"). */
    default String profilerChartPath() { return "root"; }

    default void setProfilerChartPath(String path) {}

    /** Rebuild chat lines (e.g. when Chat Heads is switched on or off). */
    default void refreshChat() {}

    /** Number of entries below the circle (for the height of the chart). */
    default int profilerChartEntries() { return 7; }

    /** Whether the game window has focus (no longer after Alt+Tab). */
    default boolean windowFocused() { return true; }

    /** Whether a key name matches the current binding of a vanilla key. */
    boolean matchesGameKey(GameKey gameKey, String key);

    /** Sets the pressed state of a vanilla key, e.g. for Toggle Sprint. */
    void setGameKeyPressed(GameKey key, boolean pressed);

    /** Readable, localised name of a key. */
    String keyName(String key);

    // ---- Data for the item browser (trades, loot, world generation) ----

    /**
     * All JSON files of a data pack folder by id, e.g. {@code loot_table} →
     * {@code minecraft:chests/desert_pyramid}. May be called from a background thread.
     */
    default Map<String, JsonElement> dataFiles(String dir) { return Map.of(); }

    /** Item by id with count; {@link ItemRef#EMPTY} if the item does not exist. */
    default ItemRef item(String id, int count) { return ItemRef.EMPTY; }

    /** Translation of a language key or {@code fallback}. */
    default String translate(String key, String fallback) { return fallback; }

    /** Villager professions with workstation and trade sets. */
    default List<Profession> professions() { return List.of(); }

    /** Preview of armor trims for a smithing template (empty if none). */
    default List<RecipeView> trimViews(ItemRef template) { return List.of(); }

    /**
     * Trade offers of known villagers nearby (known once their trade window
     * was opened once), position above the head, interpolated.
     */
    default List<WorldLabel> villagerLabels(double maxDistance, float partialTick) { return List.of(); }

    /**
     * FPS boost: apply vanilla settings for the desired optimisations. Ones that are no longer wanted
     * are reset to their original value.
     *
     * @param saved bisher gespeicherte Ursprungswerte (vom Adapter kodiert)
     * @return new original values to save, null = unchanged (e.g. options not loaded yet)
     */
    default String applyFpsBoost(Set<String> tweaks, String saved) { return null; }

    /** FPS boost off: restore original values. */
    default void restoreFpsBoost(String saved) {}

    /** Switch freecam on/off (camera detaches from the player). */
    default void setFreecam(boolean on) {}

    default boolean freecamActive() { return false; }

    /** IDs aller Partikelarten, z. B. {@code minecraft:crit}. */
    default List<String> particleTypes() { return List.of(); }

    /** Whether an inventory/container screen is open (not chat, menu, ...). */
    default boolean containerScreenOpen() { return false; }

    /** Hunger, saturation, health and the food in hand (food info). */
    default FoodState foodState() { return FoodState.NONE; }

    /** All key bindings of Minecraft and mods (controls). */
    default List<KeyBinding> keyBindings() { return List.of(); }

    /** Rebinds a Minecraft/mod key and saves the options. Empty = unbound. */
    default void setKeyBinding(String id, String key) {}

    // ---- Kamera ----
    Perspective perspective();

    void setPerspective(Perspective perspective);

    // ---- UI ----
    void openScreen(UiScreen screen);

    void closeScreen();

    boolean isScreenOpen();

    /** Whether one of our own screens is currently open. */
    boolean isOwnScreenOpen();

    String clipboard();

    void setClipboard(String text);

    /** Short message for the player (e.g. action bar). */
    void notify(String message);
}
