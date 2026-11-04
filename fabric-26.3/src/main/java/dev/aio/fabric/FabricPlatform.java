package dev.aio.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import dev.aio.core.platform.EffectInfo;
import dev.aio.core.platform.GameKey;
import dev.aio.core.platform.ItemRef;
import dev.aio.core.platform.Keys;
import dev.aio.core.platform.LookTarget;
import dev.aio.core.platform.Perspective;
import dev.aio.core.platform.Platform;
import dev.aio.core.platform.Vec3;
import dev.aio.core.platform.ContainerView;
import dev.aio.core.platform.RecipeView;
import dev.aio.core.platform.ServerInfo;
import dev.aio.core.platform.VanillaOption;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import dev.aio.core.ui.UiScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.ToggleKeyMapping;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** {@link Platform}-Implementierung für Minecraft 26.3 (offizielle Mojang-Namen). */
public final class FabricPlatform implements Platform {
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private final Minecraft client;
    private UiScreen pendingScreen;
    private boolean pendingClose;

    public FabricPlatform(Minecraft client) {
        this.client = client;
    }

    /** MC-Tastenname zu einem Tastatur-Event, z. B. {@code key.keyboard.right.shift}. */
    public static String keyName(KeyEvent event) {
        return InputConstants.getKey(event).getName();
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

    /** "minecraft:speed" -> "minecraft:mob_effect/speed" (Sprite im GUI-Atlas). */
    private static String effectSprite(String id) {
        int colon = id.indexOf(':');
        return colon < 0 ? "minecraft:mob_effect/" + id : id.substring(0, colon) + ":mob_effect/" + id.substring(colon + 1);
    }

    // ---- Blick-Info ----

    private final Map<String, String> modNames = new ConcurrentHashMap<>();

    /** Anzeigename einer Mod zu einem Namespace ("minecraft" -> "Minecraft"). */
    private String modName(String namespace) {
        return modNames.computeIfAbsent(namespace, ns -> FabricLoader.getInstance().getModContainer(ns)
                .map(c -> c.getMetadata().getName())
                .orElse(ns.isEmpty() ? ns : Character.toUpperCase(ns.charAt(0)) + ns.substring(1)));
    }

    @Override public LookTarget lookTarget() {
        LocalPlayer p = player();
        HitResult hit = client.hitResult;
        if (p == null || client.level == null || hit == null || hit.getType() == HitResult.Type.MISS) return null;

        if (hit instanceof EntityHitResult eh) {
            Entity e = eh.getEntity();
            String source = modName(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getNamespace());
            ItemRef icon = e instanceof ItemEntity item ? FabricItemRef.of(item.getItem()) : ItemRef.EMPTY;
            BlockPos pos = e.blockPosition();
            String type = e.getType().getDescription().getString();
            if (e instanceof LivingEntity living) {
                return LookTarget.entity(e.getDisplayName().getString(), type, source, icon,
                        living.getHealth(), living.getMaxHealth(), living.getArmorValue(), pos.getX(), pos.getY(), pos.getZ());
            }
            return LookTarget.entity(e.getDisplayName().getString(), type, source, icon, Float.NaN, Float.NaN, -1,
                    pos.getX(), pos.getY(), pos.getZ());
        }

        if (hit instanceof BlockHitResult bh) {
            BlockPos pos = bh.getBlockPos();
            BlockState state = client.level.getBlockState(pos);
            if (state.isAir()) return null;
            String source = modName(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace());
            ItemRef icon = FabricItemRef.of(state.getCloneItemStack(client.level, pos, false));

            String tool = toolType(state);
            Boolean correct = null;
            if (state.requiresCorrectToolForDrops()) {
                correct = p.hasCorrectToolForDrops(state); // ohne passendes Werkzeug kein Drop
            } else if (tool != null && p.getMainHandItem().getDestroySpeed(state) > 1f) {
                correct = Boolean.TRUE; // passendes Werkzeug in der Hand (abbaubar ist es auch ohne)
            }

            return LookTarget.block(state.getBlock().getName().getString(), source, icon, correct, tool, growth(state),
                    pos.getX(), pos.getY(), pos.getZ(), BlockDetails.of(client, state, pos));
        }
        return null;
    }

    /** Wachstum 0..1, nur für Pflanzen, die wirklich wachsen (nicht Zuckerrohr, Kaktus, Feuer, ...). */
    private static float growth(BlockState state) {
        var block = state.getBlock();
        if (block instanceof CropBlock crop) {
            return crop.getMaxAge() > 0 ? crop.getAge(state) / (float) crop.getMaxAge() : Float.NaN;
        }
        if (block instanceof NetherWartBlock || block instanceof SweetBerryBushBlock) return age(state, BlockStateProperties.AGE_3, 3);
        if (block instanceof CocoaBlock) return age(state, BlockStateProperties.AGE_2, 2);
        if (block instanceof StemBlock) return age(state, BlockStateProperties.AGE_7, 7);
        if (block instanceof SaplingBlock) return age(state, BlockStateProperties.STAGE, 1);
        return Float.NaN;
    }

    private static float age(BlockState state, IntegerProperty property, int max) {
        return state.hasProperty(property) ? state.getValue(property) / (float) max : Float.NaN;
    }

    private static String toolType(BlockState state) {
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) return "Spitzhacke";
        if (state.is(BlockTags.MINEABLE_WITH_AXE)) return "Axt";
        if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) return "Schaufel";
        if (state.is(BlockTags.MINEABLE_WITH_HOE)) return "Hacke";
        return null;
    }

    // ---- Welt ----

    @Override public String worldKey() {
        var server = client.getSingleplayerServer();
        if (server != null) return "sp:" + server.getWorldData().getLevelName();
        ServerData info = client.getCurrentServer();
        return info == null ? "unbekannt" : "server:" + info.ip.toLowerCase(java.util.Locale.ROOT);
    }

    @Override public String playerName() { return player() == null ? "" : player().getName().getString(); }

    @Override public long dayTime() { return client.level == null ? 0 : client.level.getOverworldClockTime(); }

    @Override public String dimension() {
        if (client.level == null) return "";
        var dim = client.level.dimension();
        if (dim == Level.OVERWORLD) return "overworld";
        if (dim == Level.NETHER) return "the_nether";
        if (dim == Level.END) return "the_end";
        return dim.toString();
    }

    @Override public Vec3 eyePos() {
        LocalPlayer p = player();
        if (p == null) return Vec3.ZERO;
        var eye = p.getEyePosition();
        return new Vec3(eye.x, eye.y, eye.z);
    }

    @Override public double fov() { return client.options.fov().get(); }

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

    private final java.util.Map<String, TagKey<Item>> tagCache = new HashMap<>();

    @Override public int countItems(String idOrTag) {
        LocalPlayer p = player();
        if (p == null) return 0;
        Inventory inv = p.getInventory();
        boolean tag = idOrTag.startsWith("#");
        TagKey<Item> key = tag ? tagCache.computeIfAbsent(idOrTag, t -> TagKey.create(Registries.ITEM, Identifier.parse(t.substring(1)))) : null;
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            if (tag ? s.is(key) : BuiltInRegistries.ITEM.getKey(s.getItem()).toString().equals(idOrTag)) n += s.getCount();
        }
        return n;
    }

    @Override public ItemRef item(String id) {
        return BuiltInRegistries.ITEM.get(Identifier.parse(id))
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
            out.add(Enchantment.getFullname(e.getKey(), e.getIntValue()).getString());
        }
        return out;
    }

    // ---- Hotbar ----

    @Override public int selectedSlot() { return player() == null ? 0 : player().getInventory().getSelectedSlot(); }

    /** Der Wechsel geht mit der nächsten Aktion (Abbauen, Schlagen) bzw. im nächsten Tick an den Server. */
    @Override public void selectSlot(int slot) {
        if (player() != null && slot >= 0 && slot < 9) player().getInventory().setSelectedSlot(slot);
    }

    @Override public int bestToolSlot() {
        LocalPlayer p = player();
        if (p == null || client.level == null || !(client.hitResult instanceof BlockHitResult bh)) return -1;
        BlockState state = client.level.getBlockState(bh.getBlockPos());
        if (state.isAir()) return -1;
        int current = p.getInventory().getSelectedSlot();
        int best = -1;
        float bestScore = toolScore(p.getInventory().getItem(current), state);
        for (int i = 0; i < 9; i++) {
            float score = toolScore(p.getInventory().getItem(i), state);
            if (score > bestScore + 0.01f) {
                bestScore = score;
                best = i;
            }
        }
        return best;
    }

    private static float toolScore(ItemStack stack, BlockState state) {
        float speed = stack.getDestroySpeed(state);
        if (state.requiresCorrectToolForDrops() && stack.isCorrectToolForDrops(state)) speed += 1000;
        // fast kaputte Werkzeuge schonen
        if (stack.isDamageableItem() && stack.getMaxDamage() - stack.getDamageValue() <= 2) speed -= 2000;
        return speed;
    }

    @Override public int bestWeaponSlot(boolean preferAxe) {
        LocalPlayer p = player();
        if (p == null) return -1;
        int best = -1;
        int bestScore = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack s = p.getInventory().getItem(i);
            int type = s.is(ItemTags.SWORDS) ? (preferAxe ? 1 : 2) : s.is(ItemTags.AXES) ? (preferAxe ? 2 : 1) : 0;
            if (type == 0) continue;
            // höhere Stufe = mehr Haltbarkeit (Holz < Stein < Eisen < Diamant < Netherit)
            int score = type * 100_000 + s.getMaxDamage();
            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
        }
        return best;
    }

    // ---- Inventar / Item-Liste ----

    @Override public ContainerView container() {
        if (client.gui.screen() instanceof AbstractContainerScreen<?> screen && player() != null && client.gameMode != null) {
            return new FabricContainerView(client, screen.getMenu());
        }
        return null;
    }

    private List<ItemRef> allItemsCache;

    @Override public List<ItemRef> allItems() {
        if (allItemsCache == null) {
            List<ItemRef> list = new ArrayList<>();
            for (Item item : BuiltInRegistries.ITEM) {
                ItemStack stack = new ItemStack(item);
                if (!stack.isEmpty()) list.add(FabricItemRef.of(stack));
            }
            list.sort(java.util.Comparator.comparing(ItemRef::id));
            allItemsCache = List.copyOf(list);
        }
        return allItemsCache;
    }

    @Override public List<RecipeView> recipes(ItemRef item, boolean usages) { return RecipeSource.find(client, item, usages); }

    @Override public boolean recipesAvailable() { return RecipeSource.available(client); }

    // ---- Optionen ----

    private long optionsDirtyAt = -1;

    @SuppressWarnings("unchecked")
    private OptionInstance<?> optionInstance(VanillaOption o) {
        Options op = client.options;
        return switch (o) {
            case CHAT_WIDTH -> op.chatWidth();
            case CHAT_HEIGHT_FOCUSED -> op.chatHeightFocused();
            case CHAT_HEIGHT_UNFOCUSED -> op.chatHeightUnfocused();
            case CHAT_SCALE -> op.chatScale();
            case CHAT_LINE_SPACING -> op.chatLineSpacing();
            case CHAT_OPACITY -> op.chatOpacity();
            case TEXT_BACKGROUND_OPACITY -> op.textBackgroundOpacity();
            case FORCE_UNICODE_FONT -> op.forceUnicodeFont();
        };
    }

    @Override public double option(VanillaOption o) {
        Object v = optionInstance(o).get();
        if (v instanceof Boolean b) return b ? 1 : 0;
        return v instanceof Number n ? n.doubleValue() : 0;
    }

    @SuppressWarnings("unchecked")
    @Override public void setOption(VanillaOption o, double value) {
        if (o == VanillaOption.FORCE_UNICODE_FONT) {
            OptionInstance<Boolean> opt = (OptionInstance<Boolean>) optionInstance(o);
            boolean b = value > 0.5;
            if (opt.get() != b) opt.set(b);
        } else {
            ((OptionInstance<Double>) optionInstance(o)).set(Math.max(0, Math.min(1, value)));
        }
        optionsDirtyAt = System.currentTimeMillis();
    }

    /** Vom Tick aufgerufen: Optionen gesammelt speichern (nicht bei jedem Zieh-Schritt). */
    void saveOptionsIfDirty() {
        if (optionsDirtyAt > 0 && System.currentTimeMillis() - optionsDirtyAt > 1000) {
            optionsDirtyAt = -1;
            client.options.save();
        }
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

    @Override public boolean isGameKeyDown(GameKey key) { return mapping(key).isDown(); }

    @Override public String gameKeyName(GameKey key) {
        String name = mapping(key).getTranslatedKeyMessage().getString();
        return name.length() > 4 ? name.substring(0, 4) : name;
    }

    @Override public boolean isKeyDown(String key) { return InputState.isDown(key); }

    @Override public boolean matchesGameKey(GameKey gameKey, String key) {
        return !Keys.isNone(key) && key.equals(mapping(gameKey).saveString());
    }

    @Override public void setGameKeyPressed(GameKey key, boolean pressed) {
        KeyMapping m = mapping(key);
        // Bei aktivierter Vanilla-Option "Umschalten" würde setDown jedes Mal umschalten.
        if (m instanceof ToggleKeyMapping) return;
        m.setDown(pressed);
    }

    @Override public String keyName(String key) {
        if (Keys.isNone(key)) return "Keine";
        try {
            return InputConstants.getKey(key).getDisplayName().getString();
        } catch (RuntimeException e) {
            return key;
        }
    }

    // ---- Kamera ----

    @Override public Perspective perspective() {
        return switch (client.options.getCameraType()) {
            case FIRST_PERSON -> Perspective.FIRST_PERSON;
            case THIRD_PERSON_BACK -> Perspective.THIRD_PERSON_BACK;
            case THIRD_PERSON_FRONT -> Perspective.THIRD_PERSON_FRONT;
        };
    }

    @Override public void setPerspective(Perspective perspective) {
        client.options.setCameraType(switch (perspective) {
            case FIRST_PERSON -> CameraType.FIRST_PERSON;
            case THIRD_PERSON_BACK -> CameraType.THIRD_PERSON_BACK;
            case THIRD_PERSON_FRONT -> CameraType.THIRD_PERSON_FRONT;
        });
    }

    // ---- UI ----

    /**
     * Wird erst im nächsten Client-Tick ausgeführt: Das Öffnen passiert oft mitten in der
     * Tastatur- oder Mausverarbeitung, die sonst denselben Tastendruck an den neuen Screen weitergibt.
     */
    @Override public void openScreen(UiScreen screen) {
        // wenn schon ein Tick-Wechsel aussteht, gewinnt der neueste
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
            client.gui.setScreen(new AioScreen(s, client.gui.screen()));
        } else if (pendingClose) {
            pendingClose = false;
            if (client.gui.screen() instanceof AioScreen) client.gui.setScreen(null);
        }
    }

    @Override public boolean isScreenOpen() { return client.gui.screen() != null; }

    @Override public boolean isOwnScreenOpen() { return client.gui.screen() instanceof AioScreen; }

    @Override public String clipboard() { return client.keyboardHandler.getClipboard(); }

    @Override public void setClipboard(String text) { client.keyboardHandler.setClipboard(text); }

    @Override public void notify(String message) {
        LocalPlayer p = client.player;
        if (p == null) {
            AioFabricClient.LOGGER.info(message);
            return;
        }
        p.sendOverlayMessage(Component.literal("AIO » ").withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.literal(message).withStyle(ChatFormatting.WHITE)));
    }
}
