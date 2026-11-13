package dev.aio.fabric;

import dev.aio.core.AioCore;
import dev.aio.core.modules.render.FreelookModule;
import dev.aio.core.platform.CameraView;
import dev.aio.core.platform.ContainerView;
import dev.aio.core.platform.EffectInfo;
import dev.aio.core.platform.FoodState;
import dev.aio.core.platform.GameKey;
import dev.aio.core.platform.HealthTarget;
import dev.aio.core.platform.ItemRef;
import dev.aio.core.platform.KeyBinding;
import dev.aio.core.platform.Keys;
import dev.aio.core.platform.LookTarget;
import dev.aio.core.platform.NametagTarget;
import dev.aio.core.platform.Perspective;
import dev.aio.core.platform.Platform;
import dev.aio.core.platform.Profession;
import dev.aio.core.platform.RecipeView;
import dev.aio.core.platform.Renderer;
import dev.aio.core.platform.ServerInfo;
import dev.aio.core.platform.VanillaOption;
import dev.aio.core.platform.Vec3;
import dev.aio.core.platform.WorldLabel;
import dev.aio.core.ui.UiScreen;
import dev.aio.fabric.compat.HookStatus;
import dev.aio.fabric.compat.ModConflicts;
import dev.aio.fabric.mixin.FoodDataAccessor;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.platform.InputConstants;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.ToggleKeyMapping;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.EntityHitResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** {@link Platform} implementation for Minecraft 26.3 (official Mojang names). */
public final class FabricPlatform implements Platform {
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private final Minecraft client;
    private UiScreen pendingScreen;
    private boolean pendingClose;

    private final LookTargets lookTargets;
    private final HotbarTools hotbarTools;
    private final VanillaOptions options;

    public FabricPlatform(Minecraft client) {
        this.client = client;
        this.lookTargets = new LookTargets(client);
        this.hotbarTools = new HotbarTools(client);
        this.options = new VanillaOptions(client);
    }

    /** MC-Tastenname zu einem Tastatur-Event, z. B. {@code key.keyboard.right.shift}. */
    public static String keyName(int keyCode, int scanCode) {
        return InputConstants.getKey(keyCode, scanCode).getName();
    }

    /**
     * Translates the MC mouse button into the core's numbering (0 = left, 1 = right, 2 = middle).
     * 26.3 uses SDL, so the raw values do not have to match the old GLFW values.
     */
    public static int button(int raw) {
        if (raw == InputConstants.MOUSE_BUTTON_LEFT) return 0;
        if (raw == InputConstants.MOUSE_BUTTON_RIGHT) return 1;
        if (raw == InputConstants.MOUSE_BUTTON_MIDDLE) return 2;
        return raw < 3 ? 3 + raw : raw; // side buttons: never confuse with 0..2
    }

    private LocalPlayer player() { return client.player; }

    // ---- Zustand ----

    @Override public boolean inWorld() { return client.player != null && client.level != null; }

    @Override public boolean isSingleplayer() { return client.hasSingleplayerServer(); }

    @Override public String serverAddress() {
        ServerData info = client.getCurrentServer();
        return info == null ? null : info.ip;
    }

    @Override public int fps() { return client.getFps(); }

    @Override public int ping() {
        LocalPlayer p = player();
        if (p == null || client.getConnection() == null) return 0;
        PlayerInfo info = client.getConnection().getPlayerInfo(p.getUUID());
        return info == null ? 0 : info.getLatency();
    }

    @Override public Vec3 playerPos() {
        LocalPlayer p = player();
        return p == null ? Vec3.ZERO : new Vec3(p.getX(), p.getY(), p.getZ());
    }

    @Override public float playerYaw() { return player() == null ? 0 : player().getYRot(); }

    @Override public float playerPitch() { return player() == null ? 0 : player().getXRot(); }

    @Override public String biome() {
        LocalPlayer p = player();
        if (p == null || client.level == null) return "";
        String id = client.level.getBiome(p.blockPosition()).getRegisteredName();
        return Component.translatable("biome." + id.replace(':', '.')).getString();
    }

    @Override public List<ItemRef> armor() {
        LocalPlayer p = player();
        List<ItemRef> list = new ArrayList<>(4);
        for (EquipmentSlot slot : ARMOR) list.add(p == null ? ItemRef.EMPTY : FabricItemRef.of(p.getItemBySlot(slot)));
        return list;
    }

    @Override public ItemRef mainHand() { return player() == null ? ItemRef.EMPTY : FabricItemRef.of(player().getMainHandItem()); }

    @Override public ItemRef offHand() { return player() == null ? ItemRef.EMPTY : FabricItemRef.of(player().getOffhandItem()); }

    @Override public List<EffectInfo> effects() {
        LocalPlayer p = player();
        if (p == null) return List.of();
        List<EffectInfo> list = new ArrayList<>();
        for (MobEffectInstance inst : p.getActiveEffects()) {
            MobEffect effect = inst.getEffect().value();
            list.add(new EffectInfo(
                    effect.getDisplayName().getString(),
                    inst.getAmplifier(),
                    inst.getDuration(),
                    inst.isInfiniteDuration(),
                    effect.getCategory() == MobEffectCategory.BENEFICIAL,
                    effectSprite(inst.getEffect().getRegisteredName())));
        }
        return list;
    }

    /** "minecraft:speed" -> "minecraft:mob_effect/speed" (sprite in the GUI atlas). */
    private static String effectSprite(String id) {
        int colon = id.indexOf(':');
        return colon < 0 ? "minecraft:mob_effect/" + id : id.substring(0, colon) + ":mob_effect/" + id.substring(colon + 1);
    }

    @Override public LookTarget lookTarget(double maxDistance) { return lookTargets.find(maxDistance); }

    // ---- Welt ----

    @Override public String worldKey() {
        var server = client.getSingleplayerServer();
        if (server != null) {
            // Folder name is unique, the display name is not
            var root = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
            var folder = root.getFileName();
            return "sp:" + (folder != null ? folder.toString() : server.getWorldData().getLevelName());
        }
        ServerData info = client.getCurrentServer();
        return info == null ? "unbekannt" : "server:" + info.ip.toLowerCase(Locale.ROOT);
    }

    @Override public String playerName() { return player() == null ? "" : player().getName().getString(); }

    @Override public long dayTime() { return client.level == null ? 0 : client.level.getDayTime(); }

    @Override public String dimension() {
        if (client.level == null) return "";
        var dim = client.level.dimension();
        if (dim == Level.OVERWORLD) return "overworld";
        if (dim == Level.NETHER) return "the_nether";
        if (dim == Level.END) return "the_end";
        return dim.toString();
    }

    @Override public CameraView camera(float partial) {
        LocalPlayer p = player();
        if (p == null) return new CameraView(0, 0, 0, 0, 0, 70);
        double fov0 = FabricHooks.lastFov > 1 ? FabricHooks.lastFov : (client.options == null ? 70 : client.options.fov().get());
        CameraView exact = FrameCamera.current(client, fov0);
        if (exact != null) return exact;
        var fl = AioCore.get() == null ? null : AioCore.get().module(FreelookModule.class);
        boolean free = fl != null && fl.isEngaged() && freelookSupported();
        float yaw = free ? fl.cameraYaw() : p.getViewYRot(partial);
        float pitch = free ? fl.cameraPitch() : p.getViewXRot(partial);
        var eye = p.getEyePosition(partial);
        double x = eye.x, y = eye.y, z = eye.z;
        Perspective persp = perspective();
        if (persp != Perspective.FIRST_PERSON) {
            // The vanilla camera stands 4 blocks behind (or in front of) the head; walls shorten that, ignored here
            double ry = Math.toRadians(yaw), rp = Math.toRadians(pitch);
            double fx = -Math.sin(ry) * Math.cos(rp), fy = -Math.sin(rp), fz = Math.cos(ry) * Math.cos(rp);
            double d = persp == Perspective.THIRD_PERSON_BACK ? -4 : 4;
            x += fx * d;
            y += fy * d;
            z += fz * d;
            if (persp == Perspective.THIRD_PERSON_FRONT) {
                yaw += 180;
                pitch = -pitch;
            }
        }
        double fov = FabricHooks.lastFov > 1 ? FabricHooks.lastFov : (client.options == null ? 70 : client.options.fov().get());
        return new CameraView(x, y, z, yaw, pitch, fov);
    }

    @Override public boolean freelookSupported() { return FabricHooks.cameraHookSeen; }

    @Override public boolean isModLoaded(String modId) { return FabricLoader.getInstance().isModLoaded(modId); }

    @Override public boolean crosshairOnLiving() {
        return client.hitResult instanceof EntityHitResult eh && eh.getEntity() instanceof LivingEntity le
                && le.isAlive() && !(le instanceof ArmorStand);
    }

    @Override public boolean isDead() { return player() != null && player().isDeadOrDying(); }

    @Override public long playTimeTicks() {
        LocalPlayer p = player();
        if (p == null) return -1;
        int v = p.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
        return v > 0 ? v : -1;
    }

    @Override public void requestStats() {
        if (client.getConnection() != null) {
            client.getConnection().send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS));
        }
    }

    @Override public ServerInfo serverInfo() {
        ServerData info = client.getCurrentServer();
        var conn = client.getConnection();
        if (info == null || conn == null) return null;
        String brand = conn.serverBrand();
        return new ServerInfo(info.name == null ? "" : info.name, info.ip, ping(), conn.getOnlinePlayers().size(),
                info.version == null ? "" : info.version.getString(), info.motd == null ? "" : info.motd.getString(),
                brand == null ? "" : brand);
    }

    private final Map<String, TagKey<Item>> tagCache = new HashMap<>();

    @Override public int countItems(String idOrTag) {
        LocalPlayer p = player();
        if (p == null) return 0;
        Inventory inv = p.getInventory();
        boolean tag = idOrTag.startsWith("#");
        TagKey<Item> key = tag ? tagCache.computeIfAbsent(idOrTag, t -> TagKey.create(Registries.ITEM, ResourceLocation.parse(t.substring(1)))) : null;
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            if (tag ? s.is(key) : BuiltInRegistries.ITEM.getKey(s.getItem()).toString().equals(idOrTag)) n += s.getCount();
        }
        return n;
    }

    @Override public ItemRef item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id))
                .map(h -> FabricItemRef.of(new ItemStack(h.value())))
                .orElse(ItemRef.EMPTY);
    }

    @Override public List<String> heldItemDetails() {
        LocalPlayer p = player();
        if (p == null) return List.of();
        ItemStack s = p.getMainHandItem();
        ItemEnchantments ench = s.getEnchantments();
        if (ench.isEmpty()) ench = s.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        List<String> out = new ArrayList<>();
        for (Object2IntMap.Entry<Holder<Enchantment>> e : ench.entrySet()) {
            // Curses red like in the vanilla tooltip
            boolean curse = e.getKey().is(EnchantmentTags.CURSE);
            out.add((curse ? "§c" : "") + Enchantment.getFullname(e.getKey(), e.getIntValue()).getString());
        }
        return out;
    }

    // ---- Hotbar ----

    @Override public int selectedSlot() { return player() == null ? 0 : player().getInventory().getSelectedSlot(); }

    /** The switch goes to the server with the next action (breaking, hitting) or in the next tick. */
    @Override public void selectSlot(int slot) {
        if (player() != null && slot >= 0 && slot < 9) player().getInventory().setSelectedSlot(slot);
    }

    @Override public int bestToolSlot() { return hotbarTools.bestToolSlot(); }

    @Override public int bestWeaponSlot(boolean preferAxe) { return hotbarTools.bestWeaponSlot(preferAxe); }

    // ---- Inventar / Item-Liste ----

    /**
     * Open menu. Even when the recipe view lies over the inventory, the inventory's menu is
     * still open ({@code player.containerMenu}); without a screen it is the player inventory.
     */
    @Override public ContainerView container() {
        if (player() == null || client.gameMode == null) return null;
        if (client.screen instanceof AbstractContainerScreen<?> screen) return new FabricContainerView(client, screen.getMenu());
        return player().containerMenu == null ? null : new FabricContainerView(client, player().containerMenu);
    }

    private List<ItemRef> allItemsCache;

    @Override public List<ItemRef> allItems() {
        if (allItemsCache == null) {
            List<ItemRef> list = new ArrayList<>();
            for (Item item : BuiltInRegistries.ITEM) {
                ItemStack stack = new ItemStack(item);
                if (!stack.isEmpty()) list.add(FabricItemRef.of(stack));
            }
            list.sort(Comparator.comparing(ItemRef::id));
            allItemsCache = List.copyOf(list);
        }
        return allItemsCache;
    }

    @Override public List<RecipeView> recipes(ItemRef item, boolean usages) { return RecipeSource.find(client, item, usages); }

    @Override public boolean recipesAvailable() { return RecipeSource.available(client); }

    // ---- Optionen ----

    @Override public double option(VanillaOption o) { return options.get(o); }

    @Override public void setOption(VanillaOption o, double value) { options.set(o, value); }

    /** Vom Tick aufgerufen: Optionen gesammelt speichern. */
    void saveOptionsIfDirty() { options.saveIfDirty(); }

    @Override public void playSound(String id, float volume, float pitch) {
        SoundEvent ev = switch (id) {
            case "bell" -> SoundEvents.NOTE_BLOCK_BELL.value();
            case "anvil" -> SoundEvents.ANVIL_LAND;
            case "xp" -> SoundEvents.EXPERIENCE_ORB_PICKUP;
            case "bass" -> SoundEvents.NOTE_BLOCK_BASS.value();
            case "break" -> SoundEvents.ITEM_BREAK.value();
            default -> SoundEvents.NOTE_BLOCK_PLING.value();
        };
        client.getSoundManager().play(SimpleSoundInstance.forUI(ev, pitch, Math.max(0.05f, volume)));
    }

    @Override public void playAlertSound() {
        client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING, 2.0F));
    }

    // ---- Eingabe ----

    private KeyMapping mapping(GameKey key) {
        Options o = client.options;
        return switch (key) {
            case FORWARD -> o.keyUp;
            case BACK -> o.keyDown;
            case LEFT -> o.keyLeft;
            case RIGHT -> o.keyRight;
            case JUMP -> o.keyJump;
            case SNEAK -> o.keyShift;
            case SPRINT -> o.keySprint;
            case ATTACK -> o.keyAttack;
            case USE -> o.keyUse;
        };
    }

    @Override public boolean isGameKeyDown(GameKey key) { return client.options != null && mapping(key).isDown(); }

    @Override public String gameKeyName(GameKey key) {
        if (client.options == null) return "";
        String name = mapping(key).getTranslatedKeyMessage().getString();
        return name.length() > 4 ? name.substring(0, 4) : name;
    }

    @Override public boolean isKeyDown(String key) { return InputState.isDown(key); }

    @Override public boolean windowFocused() { return client.isWindowActive(); }

    @Override public List<HealthTarget> healthTargets(double maxDistance, float partialTick, boolean players,
                                                                         boolean hostile, boolean passive, boolean throughWalls) {
        return HealthTargets.collect(client, maxDistance, partialTick, players, hostile, passive, throughWalls);
    }

    @Override public List<NametagTarget> nametagTargets(double maxDistance, float partialTick, boolean players, boolean mobs,
                                                        boolean items, boolean tnt, boolean throughWalls) {
        return NametagTargets.collect(client, maxDistance, partialTick, players, mobs, items, tnt, throughWalls);
    }

    @Override public boolean debugScreenOpen() { return client.getDebugOverlay().showDebugScreen(); }

    @Override public boolean drawProfilerChart(Renderer r, int w, int h) {
        if (!(r instanceof FabricRenderer fr) || fr.graphics() == null || !FabricHooks.profilerResults) return false;
        var chart = client.getDebugOverlay().getProfilerPieChart();
        // In 26.2 the chart aligns itself at the bottom right of the screen: shift it so that its corner
        // lands on the local rectangle (right edge = width − 5, bottom = height)
        var g = fr.graphics();
        chart.setBottomOffset(0);
        g.pose().pushMatrix();
        g.pose().translate(w + 5 - g.guiWidth(), h - g.guiHeight());
        chart.render(g);
        g.pose().popMatrix();
        return true;
    }

    @Override public String profilerChartPath() {
        // Without the accessor (e.g. displaced by another mod) it simply stays at "root"
        return client.getDebugOverlay().getProfilerPieChart() instanceof dev.aio.fabric.mixin.ProfilerPieChartAccessor a
                ? a.aio$getPath() : "root";
    }


    @Override public void refreshChat() { ChatHeads.refresh(); }

    @Override public int profilerChartEntries() {
        if (!(client.getDebugOverlay().getProfilerPieChart() instanceof dev.aio.fabric.mixin.ProfilerPieChartAccessor a)) return 7;
        var results = a.aio$getResults();
        return results == null ? 7 : Math.max(1, results.getTimes(a.aio$getPath()).size() - 1);
    }

    @Override public void setProfilerChartPath(String path) {
        if (client.getDebugOverlay().getProfilerPieChart() instanceof dev.aio.fabric.mixin.ProfilerPieChartAccessor a) a.aio$setPath(path);
    }

    @Override public Map<String, String> conflictingMods() {
        Map<String, String> found = ModConflicts.installed();
        if (!found.isEmpty() && !conflictsLogged) {
            conflictsLogged = true;
            AioFabricClient.LOGGER.info("[aio] Other mods take priority: {}", found);
        }
        return found;
    }

    private boolean conflictsLogged;

    @Override public Map<String, String> brokenHooks() {
        List<String> missing = HookStatus.missing();
        if (!missing.isEmpty()) AioFabricClient.LOGGER.warn("[aio] Hooks not applied (changed by another mod?): {}", missing);
        return HookStatus.broken();
    }

    @Override public boolean matchesGameKey(GameKey gameKey, String key) {
        return client.options != null && !Keys.isNone(key) && key.equals(mapping(gameKey).saveString());
    }

    @Override public void setGameKeyPressed(GameKey key, boolean pressed) {
        if (client.options == null) return;
        KeyMapping m = mapping(key);
        // Sprint/sneak are always ToggleKeyMappings; only when the vanilla option "Toggle" is on
        // would setDown toggle every time (then vanilla toggles by itself anyway).
        if (m instanceof ToggleKeyMapping && vanillaToggle(key)) return;
        m.setDown(pressed);
    }

    private boolean vanillaToggle(GameKey key) {
        return switch (key) {
            case SPRINT -> client.options.toggleSprint().get();
            case SNEAK -> client.options.toggleCrouch().get();
            default -> false;
        };
    }

    // ---- Data for the item browser ----

    @Override public Map<String, JsonElement> dataFiles(String dir) { return DataFiles.read(client, dir); }

    @Override public ItemRef item(String id, int count) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null) return ItemRef.EMPTY;
        return BuiltInRegistries.ITEM.getOptional(key)
                .map(item -> new ItemStack(item, Math.max(1, count)))
                .filter(stack -> !stack.isEmpty())
                .map(stack -> (ItemRef) FabricItemRef.of(stack))
                .orElse(ItemRef.EMPTY);
    }

    @Override public String translate(String key, String fallback) {
        Language lang = Language.getInstance();
        return lang.has(key) ? lang.getOrDefault(key) : fallback;
    }

    @Override public List<Profession> professions() {
        List<Profession> out = new ArrayList<>();
        var level = client.level;
        if (level == null) return out;
        try {
            var access = level.registryAccess();
            var pois = access.lookupOrThrow(Registries.POINT_OF_INTEREST_TYPE).listElements().toList();
            access.lookupOrThrow(Registries.VILLAGER_PROFESSION).listElements().forEach(ref -> {
                var prof = ref.value();
                String jobBlock = "";
                for (var poi : pois) {
                    if (!prof.heldJobSite().test(poi) || poi.value().matchingStates().isEmpty()) continue;
                    Item item = poi.value().matchingStates().iterator().next().getBlock().asItem();
                    if (item != Items.AIR) jobBlock = BuiltInRegistries.ITEM.getKey(item).toString();
                    break;
                }
                Map<Integer, String> sets = new TreeMap<>();
                // Trade sets are data-driven only since 26.x; not available in 1.21.11
                out.add(new Profession(ref.key().location().toString(), prof.name().getString(), jobBlock, sets));
            });
        } catch (RuntimeException e) {
            AioFabricClient.LOGGER.warn("Berufe nicht lesbar: {}", e.toString());
        }
        return out;
    }

    @Override public List<RecipeView> trimViews(ItemRef template) {
        if (!(template instanceof FabricItemRef ref) || client.level == null) return List.of();
        return TrimPreview.views(client, ref.stack());
    }

    @Override public List<WorldLabel> villagerLabels(double maxDistance, float partialTick) {
        return VillagerOffers.labels(client, maxDistance, partialTick);
    }

    @Override public String applyFpsBoost(Set<String> tweaks, String saved) { return FpsBoost.apply(client, tweaks, saved); }

    @Override public void restoreFpsBoost(String saved) { FpsBoost.restore(client, saved); }

    @Override public void setFreecam(boolean on) { Freecam.set(on); }

    @Override public boolean freecamActive() { return Freecam.active(); }

    @Override public List<String> particleTypes() {
        List<String> out = new ArrayList<>();
        for (ResourceLocation id : BuiltInRegistries.PARTICLE_TYPE.keySet()) out.add(id.toString());
        return out;
    }

    @Override public boolean containerScreenOpen() { return client.screen instanceof AbstractContainerScreen<?>; }

    @Override public FoodState foodState() {
        LocalPlayer p = player();
        if (p == null || client.gameMode == null) return FoodState.NONE;
        // Hunger bar like vanilla only in survival/adventure and not on mounts with health
        boolean visible = client.gameMode.canHurtPlayer() && !(p.getVehicle() instanceof LivingEntity);
        FoodData fd = p.getFoodData();
        float exhaustion = Float.NaN;
        var server = client.getSingleplayerServer();
        if (server != null) {
            // The server does not send the exhaustion; read directly in singleplayer (only a float)
            var sp = server.getPlayerList().getPlayer(p.getUUID());
            if (sp != null) exhaustion = ((FoodDataAccessor) sp.getFoodData()).aio$exhaustion();
        }
        FoodProperties food = p.getMainHandItem().get(DataComponents.FOOD);
        if (food == null) food = p.getOffhandItem().get(DataComponents.FOOD);
        int nutrition = food == null ? 0 : food.nutrition();
        float sat = food == null ? 0 : food.saturation();
        boolean canEat = food != null && p.canEat(food.canAlwaysEat());
        return new FoodState(fd.getFoodLevel(), fd.getSaturationLevel(), exhaustion,
                p.getHealth(), p.getMaxHealth(), p.getAbsorptionAmount(), nutrition, sat, canEat, visible);
    }

    @Override public List<KeyBinding> keyBindings() {
        List<KeyBinding> out = new ArrayList<>();
        if (client.options == null) return out;
        for (KeyMapping km : client.options.keyMappings) {
            out.add(new KeyBinding(km.getName(), Component.translatable(km.getName()).getString(),
                    Component.translatable(km.getCategory()).getString(), km.saveString(), km.getDefaultKey().getName()));
        }
        return out;
    }

    @Override public void setKeyBinding(String id, String key) {
        if (client.options == null) return;
        for (KeyMapping km : client.options.keyMappings) {
            if (!km.getName().equals(id)) continue;
            km.setKey(Keys.isNone(key) ? InputConstants.UNKNOWN : InputConstants.getKey(key));
            KeyMapping.resetMapping();
            client.options.save();
            return;
        }
    }

    @Override public String keyName(String key) {
        if (Keys.isNone(key)) return "None";
        try {
            return InputConstants.getKey(key).getDisplayName().getString();
        } catch (RuntimeException e) {
            return key;
        }
    }

    // ---- Kamera ----

    @Override public Perspective perspective() {
        if (client.options == null) return Perspective.FIRST_PERSON;
        return switch (client.options.getCameraType()) {
            case FIRST_PERSON -> Perspective.FIRST_PERSON;
            case THIRD_PERSON_BACK -> Perspective.THIRD_PERSON_BACK;
            case THIRD_PERSON_FRONT -> Perspective.THIRD_PERSON_FRONT;
        };
    }

    @Override public void setPerspective(Perspective perspective) {
        if (client.options == null) return;
        client.options.setCameraType(switch (perspective) {
            case FIRST_PERSON -> CameraType.FIRST_PERSON;
            case THIRD_PERSON_BACK -> CameraType.THIRD_PERSON_BACK;
            case THIRD_PERSON_FRONT -> CameraType.THIRD_PERSON_FRONT;
        });
    }

    // ---- UI ----

    /**
     * Only runs in the next client tick: opening often happens in the middle of
     * keyboard or mouse processing, which would otherwise pass the same key press on to the new screen.
     */
    @Override public void openScreen(UiScreen screen) {
        // if a tick switch is already pending, the newest wins
        pendingScreen = screen;
        pendingClose = false;
    }

    @Override public void closeScreen() {
        pendingScreen = null;
        pendingClose = true;
    }

    /** Vom Tick-Handler aufgerufen. */
    void applyPendingScreen() {
        if (pendingScreen != null) {
            UiScreen s = pendingScreen;
            pendingScreen = null;
            client.setScreen(new AioScreen(s, client.screen));
        } else if (pendingClose) {
            pendingClose = false;
            // onClose returns to the previous screen (inventory) for recipe view & co.
            if (client.screen instanceof AioScreen screen) screen.onClose();
        }
    }

    @Override public boolean isScreenOpen() { return client.screen != null; }

    @Override public boolean isOwnScreenOpen() { return client.screen instanceof AioScreen; }

    @Override public String clipboard() { return client.keyboardHandler.getClipboard(); }

    @Override public void setClipboard(String text) { client.keyboardHandler.setClipboard(text); }

    @Override public void notify(String message) {
        LocalPlayer p = client.player;
        if (p == null) {
            AioFabricClient.LOGGER.info(message);
            return;
        }
        p.displayClientMessage(Component.literal("AIO » ").withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.literal(message).withStyle(ChatFormatting.WHITE)), true);
    }
}
