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
    public dev.aio.core.platform.LookTarget lookTarget;
    public List<ItemRef> armor = new ArrayList<>(List.of(ItemRef.EMPTY, ItemRef.EMPTY, ItemRef.EMPTY, ItemRef.EMPTY));

    @Override public boolean inWorld() { return inWorld; }
    @Override public boolean isSingleplayer() { return singleplayer; }
    @Override public String serverAddress() { return address; }
    @Override public int fps() { return fps; }
    @Override public int ping() { return 42; }
    @Override public Vec3 playerPos() { return new Vec3(x, y, z); }
    @Override public float playerYaw() { return yaw; }
    @Override public float playerPitch() { return pitch; }
    @Override public String biome() { return "plains"; }
    @Override public List<ItemRef> armor() { return armor; }
    @Override public ItemRef mainHand() { return mainHand; }
    @Override public ItemRef offHand() { return ItemRef.EMPTY; }
    @Override public List<EffectInfo> effects() { return effects; }
    @Override public dev.aio.core.platform.LookTarget lookTarget() { return lookTarget; }

    // ---- neue Plattform-Methoden ----
    public String worldKey = "sp:Testwelt";
    public String dimension = "overworld";
    public long dayTime = 1000;
    public boolean dead;
    public long playTime = -1;
    public int statRequests;
    public double x = 1.5, y = 64, z = -2.5;
    public int selected;
    public final java.util.List<Integer> slotChanges = new java.util.ArrayList<>();
    public int bestTool = -1, bestWeapon = -1;
    public dev.aio.core.platform.ContainerView container;
    public java.util.List<ItemRef> allItems = new java.util.ArrayList<>();
    public final java.util.Map<dev.aio.core.platform.VanillaOption, Double> options = new java.util.EnumMap<>(dev.aio.core.platform.VanillaOption.class);
    public int alerts;
    public java.util.List<String> heldDetails = java.util.List.of();
    public ItemRef mainHand = ItemRef.EMPTY;

    @Override public String worldKey() { return worldKey; }
    @Override public String playerName() { return "Steve"; }
    @Override public long dayTime() { return dayTime; }
    @Override public String dimension() { return dimension; }
    @Override public Vec3 eyePos() { return new Vec3(x, y + 1.62, z); }
    @Override public double fov() { return 70; }
    @Override public boolean isDead() { return dead; }
    @Override public long playTimeTicks() { return playTime; }
    @Override public void requestStats() { statRequests++; }
    @Override public dev.aio.core.platform.ServerInfo serverInfo() {
        return new dev.aio.core.platform.ServerInfo("Testserver", "play.test.net", 42, 7, "26.3", "Hallo", "Paper");
    }
    @Override public int countItems(String idOrTag) { return idOrTag.contains("totem") ? 2 : 0; }
    @Override public ItemRef item(String id) { return ItemRef.EMPTY; }
    @Override public java.util.List<String> heldItemDetails() { return heldDetails; }
    @Override public int selectedSlot() { return selected; }
    @Override public void selectSlot(int slot) { selected = slot; slotChanges.add(slot); }
    @Override public int bestToolSlot() { return bestTool; }
    @Override public int bestWeaponSlot(boolean preferAxe) { return bestWeapon; }
    @Override public dev.aio.core.platform.ContainerView container() { return container; }
    @Override public java.util.List<ItemRef> allItems() { return allItems; }
    @Override public java.util.List<dev.aio.core.platform.RecipeView> recipes(ItemRef item, boolean usages) { return java.util.List.of(); }
    @Override public boolean recipesAvailable() { return false; }
    @Override public double option(dev.aio.core.platform.VanillaOption o) { return options.getOrDefault(o, 0.5); }
    @Override public void setOption(dev.aio.core.platform.VanillaOption o, double v) { options.put(o, v); }
    @Override public void playAlertSound() { alerts++; }
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
