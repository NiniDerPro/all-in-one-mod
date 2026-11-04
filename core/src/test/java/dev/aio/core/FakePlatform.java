package dev.aio.core;

import dev.aio.core.platform.EffectInfo;
import dev.aio.core.platform.GameKey;
import dev.aio.core.platform.ItemRef;
import dev.aio.core.platform.Perspective;
import dev.aio.core.platform.Platform;
import dev.aio.core.platform.Vec3;
import dev.aio.core.ui.UiScreen;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Test-Plattform ohne Minecraft. */
public class FakePlatform implements Platform {
    public boolean inWorld = true;
    public boolean singleplayer = true;
    public String address;
    public int fps = 120;
    public Perspective perspective = Perspective.FIRST_PERSON;
    public float yaw, pitch;
    public final Set<String> heldKeys = new HashSet<>();
    public final Set<GameKey> heldGameKeys = EnumSet.noneOf(GameKey.class);
    public final Set<GameKey> forcedGameKeys = EnumSet.noneOf(GameKey.class);
    public final List<String> messages = new ArrayList<>();
    public UiScreen screen;
    public String clipboard = "";
    public List<EffectInfo> effects = new ArrayList<>();
    public List<ItemRef> armor = new ArrayList<>(List.of(ItemRef.EMPTY, ItemRef.EMPTY, ItemRef.EMPTY, ItemRef.EMPTY));

    @Override public boolean inWorld() { return inWorld; }
    @Override public boolean isSingleplayer() { return singleplayer; }
    @Override public String serverAddress() { return address; }
    @Override public int fps() { return fps; }
    @Override public int ping() { return 42; }
    @Override public Vec3 playerPos() { return new Vec3(1.5, 64, -2.5); }
    @Override public float playerYaw() { return yaw; }
    @Override public float playerPitch() { return pitch; }
    @Override public String biome() { return "plains"; }
    @Override public List<ItemRef> armor() { return armor; }
    @Override public ItemRef mainHand() { return ItemRef.EMPTY; }
    @Override public ItemRef offHand() { return ItemRef.EMPTY; }
    @Override public List<EffectInfo> effects() { return effects; }
    @Override public boolean isGameKeyDown(GameKey key) { return heldGameKeys.contains(key); }
    @Override public String gameKeyName(GameKey key) { return key.name().substring(0, 1); }
    @Override public boolean isKeyDown(String key) { return heldKeys.contains(key); }
    @Override public boolean matchesGameKey(GameKey key, String code) { return key == GameKey.SPRINT && "key.keyboard.left.control".equals(code); }
    @Override public void setGameKeyPressed(GameKey key, boolean pressed) {
        if (pressed) forcedGameKeys.add(key); else forcedGameKeys.remove(key);
    }
    @Override public String keyName(String key) { return key.isEmpty() ? "Keine" : key.substring(key.lastIndexOf('.') + 1).toUpperCase(); }
    @Override public Perspective perspective() { return perspective; }
    @Override public void setPerspective(Perspective p) { perspective = p; }
    @Override public void openScreen(UiScreen s) { if (screen != null) screen.onClose(); screen = s; }
    @Override public void closeScreen() { if (screen != null) screen.onClose(); screen = null; }
    @Override public boolean isScreenOpen() { return screen != null; }
    @Override public boolean isOwnScreenOpen() { return screen != null; }
    @Override public String clipboard() { return clipboard; }
    @Override public void setClipboard(String text) { clipboard = text; }
    @Override public void notify(String message) { messages.add(message); }
}
