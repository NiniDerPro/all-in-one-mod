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
