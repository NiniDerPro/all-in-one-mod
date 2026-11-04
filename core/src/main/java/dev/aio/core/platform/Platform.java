package dev.aio.core.platform;

import dev.aio.core.ui.UiScreen;

import java.util.List;

/**
 * Alles, was der Kern über das Spiel wissen oder darin ändern muss.
 *
 * <p>Das ist die Versions-Abstraktionsschicht: Für eine neue MC-Version wird nur diese
 * Schnittstelle (plus {@link Renderer}, {@link ItemRef} und eine Handvoll Mixins) neu implementiert.
 */
public interface Platform {
    // ---- Zustand ----
    boolean inWorld();

    boolean isSingleplayer();

    /** Serveradresse (z. B. {@code play.example.net}) oder null im Singleplayer/Menü. */
    String serverAddress();

    int fps();

    int ping();

    Vec3 playerPos();

    float playerYaw();

    float playerPitch();

    String biome();

    /** Rüstung von Kopf bis Füße, immer 4 Einträge. */
    List<ItemRef> armor();

    ItemRef mainHand();

    ItemRef offHand();

    List<EffectInfo> effects();

    /** Block oder Wesen unter dem Fadenkreuz, null wenn nichts. */
    LookTarget lookTarget();

    // ---- Welt ----

    /** Eindeutiger Schlüssel der aktuellen Welt (Serveradresse oder Singleplayer-Weltname). */
    String worldKey();

    String playerName();

    /** Zeit der Oberwelt-Uhr in Ticks (Tag = 24000). */
    long dayTime();

    /** "overworld", "the_nether", "the_end" oder die ID einer anderen Dimension. */
    String dimension();

    /** Augenposition des Spielers (für Wegpunkt-Marker). */
    Vec3 eyePos();

    /** Eingestelltes Sichtfeld in Grad (vertikal). */
    double fov();

    boolean isDead();

    /** Spielzeit laut Statistik in Ticks, -1 wenn unbekannt. */
    long playTimeTicks();

    /** Fordert aktuelle Statistiken vom Server an (wie das Statistik-Menü). */
    void requestStats();

    ServerInfo serverInfo();

    /** Zählt Items im Inventar: Registry-ID ({@code minecraft:arrow}) oder Tag ({@code #minecraft:arrows}). */
    int countItems(String idOrTag);

    /** Item-Icon zu einer Registry-ID. */
    ItemRef item(String id);

    /** Ausgewähltes Item in der Hotbar hat sich geändert, Rückgabe: Infozeilen (Verzauberungen o. Ä.). */
    java.util.List<String> heldItemDetails();

    // ---- Hotbar ----

    int selectedSlot();

    void selectSlot(int slot);

    /** Bester Hotbar-Slot zum Abbauen des angesehenen Blocks, -1 wenn keiner besser ist. */
    int bestToolSlot();

    /** Bester Hotbar-Slot zum Kämpfen, -1 wenn keine Waffe da ist. */
    int bestWeaponSlot(boolean preferAxe);

    // ---- Inventar / Item-Liste ----

    /** Das gerade offene Inventar oder null. */
    ContainerView container();

    /** Alle Items aus der Registry (für die Item-Liste). */
    java.util.List<ItemRef> allItems();

    /** Rezepte, die {@code item} herstellen ({@code usages = true}: die es verwenden). Kann leer sein. */
    java.util.List<RecipeView> recipes(ItemRef item, boolean usages);

    /** Ob Rezepte verfügbar sind (Singleplayer oder vom Server synchronisiert). */
    boolean recipesAvailable();

    // ---- Optionen / Sonstiges ----

    double option(VanillaOption option);

    void setOption(VanillaOption option, double value);

    /** Kurzer Hinweiston (z. B. Namens-Alarm). */
    void playAlertSound();

    // ---- Eingabe ----
    boolean isGameKeyDown(GameKey key);

    /** Anzeigename der aktuellen Belegung einer Vanilla-Taste, z. B. "W" oder "LMB". */
    String gameKeyName(GameKey key);

    /** Prüft, ob eine Taste oder Maustaste (MC-Tastenname, siehe {@link Keys}) gerade gehalten wird. */
    boolean isKeyDown(String key);

    /** Ob ein Tastenname der aktuellen Belegung einer Vanilla-Taste entspricht. */
    boolean matchesGameKey(GameKey gameKey, String key);

    /** Setzt den gedrückt-Zustand einer Vanilla-Taste, z. B. für Toggle Sprint. */
    void setGameKeyPressed(GameKey key, boolean pressed);

    /** Lesbarer, lokalisierter Name einer Taste. */
    String keyName(String key);

    // ---- Kamera ----
    Perspective perspective();

    void setPerspective(Perspective perspective);

    // ---- UI ----
    void openScreen(UiScreen screen);

    void closeScreen();

    boolean isScreenOpen();

    /** Ob gerade einer unserer eigenen Screens offen ist. */
    boolean isOwnScreenOpen();

    String clipboard();

    void setClipboard(String text);

    /** Kurze Meldung für den Spieler (z. B. Action-Bar). */
    void notify(String message);
}
