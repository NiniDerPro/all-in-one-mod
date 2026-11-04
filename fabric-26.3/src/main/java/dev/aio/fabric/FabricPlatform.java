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
        String name = client.level.getBiome(p.blockPosition()).getRegisteredName();
        int colon = name.indexOf(':');
        return colon >= 0 ? name.substring(colon + 1) : name;
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
                    effect.getCategory() == MobEffectCategory.BENEFICIAL));
        }
        return list;
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
            if (e instanceof LivingEntity living) {
                return LookTarget.entity(e.getDisplayName().getString(), source, icon,
                        living.getHealth(), living.getMaxHealth(), living.getArmorValue(), pos.getX(), pos.getY(), pos.getZ());
            }
            return LookTarget.entity(e.getDisplayName().getString(), source, icon, Float.NaN, Float.NaN, -1,
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
            client.gui.setScreen(new AioScreen(s));
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
