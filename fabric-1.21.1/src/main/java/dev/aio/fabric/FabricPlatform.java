package dev.aio.fabric;

import dev.aio.core.platform.EffectInfo;
import dev.aio.core.platform.GameKey;
import dev.aio.core.platform.ItemRef;
import dev.aio.core.platform.Keys;
import dev.aio.core.platform.Perspective;
import dev.aio.core.platform.Platform;
import dev.aio.core.platform.Vec3;
import dev.aio.core.ui.UiScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.StickyKeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** {@link Platform}-Implementierung für Minecraft 1.21.1 (Yarn-Mappings). */
public final class FabricPlatform implements Platform {
    private final MinecraftClient client;

    public FabricPlatform(MinecraftClient client) {
        this.client = client;
    }

    private ClientPlayerEntity player() { return client.player; }

    // ---- Zustand ----

    @Override public boolean inWorld() { return client.player != null && client.world != null; }

    @Override public boolean isSingleplayer() { return client.isInSingleplayer(); }

    @Override public String serverAddress() {
        ServerInfo info = client.getCurrentServerEntry();
        return info == null ? null : info.address;
    }

    @Override public int fps() { return client.getCurrentFps(); }

    @Override public int ping() {
        ClientPlayerEntity p = player();
        if (p == null || client.getNetworkHandler() == null) return 0;
        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(p.getUuid());
        return entry == null ? 0 : entry.getLatency();
    }

    @Override public Vec3 playerPos() {
        ClientPlayerEntity p = player();
        return p == null ? Vec3.ZERO : new Vec3(p.getX(), p.getY(), p.getZ());
    }

    @Override public float playerYaw() { return player() == null ? 0 : player().getYaw(); }

    @Override public float playerPitch() { return player() == null ? 0 : player().getPitch(); }

    @Override public String biome() {
        ClientPlayerEntity p = player();
        if (p == null || client.world == null) return "";
        return client.world.getBiome(p.getBlockPos()).getKey()
                .map(k -> k.getValue().getPath())
                .orElse("?");
    }

    @Override public List<ItemRef> armor() {
        ClientPlayerEntity p = player();
        List<ItemRef> list = new ArrayList<>(4);
        for (int slot = 3; slot >= 0; slot--) { // 3 = Kopf ... 0 = Füße
            list.add(p == null ? ItemRef.EMPTY : FabricItemRef.of(p.getInventory().getArmorStack(slot)));
        }
        return list;
    }

    @Override public ItemRef mainHand() { return player() == null ? ItemRef.EMPTY : FabricItemRef.of(player().getMainHandStack()); }

    @Override public ItemRef offHand() { return player() == null ? ItemRef.EMPTY : FabricItemRef.of(player().getOffHandStack()); }

    @Override public List<EffectInfo> effects() {
        ClientPlayerEntity p = player();
        if (p == null) return List.of();
        List<EffectInfo> list = new ArrayList<>();
        for (StatusEffectInstance inst : p.getStatusEffects()) {
            StatusEffect effect = inst.getEffectType().value();
            list.add(new EffectInfo(
                    effect.getName().getString(),
                    inst.getAmplifier(),
                    inst.getDuration(),
                    inst.isInfinite(),
                    effect.getCategory() == StatusEffectCategory.BENEFICIAL));
        }
        return list;
    }

    // ---- Eingabe ----

    private KeyBinding binding(GameKey key) {
        GameOptions o = client.options;
        return switch (key) {
            case FORWARD -> o.forwardKey;
            case BACK -> o.backKey;
            case LEFT -> o.leftKey;
            case RIGHT -> o.rightKey;
            case JUMP -> o.jumpKey;
            case SNEAK -> o.sneakKey;
            case SPRINT -> o.sprintKey;
            case ATTACK -> o.attackKey;
            case USE -> o.useKey;
        };
    }

    @Override public boolean isGameKeyDown(GameKey key) { return binding(key).isPressed(); }

    @Override public String gameKeyName(GameKey key) {
        String name = binding(key).getBoundKeyLocalizedText().getString();
        return name.length() > 4 ? name.substring(0, 4) : name;
    }

    @Override public boolean isKeyDown(int code) {
        if (code == Keys.NONE) return false;
        long handle = client.getWindow().getHandle();
        if (Keys.isMouse(code)) return GLFW.glfwGetMouseButton(handle, Keys.mouseButton(code)) == GLFW.GLFW_PRESS;
        return InputUtil.isKeyPressed(handle, code);
    }

    @Override public boolean matchesGameKey(GameKey key, int code) {
        return binding(key).matchesKey(code, -1);
    }

    @Override public void setGameKeyPressed(GameKey key, boolean pressed) {
        KeyBinding b = binding(key);
        // Bei aktivierter Vanilla-Option "Umschalten" würde setPressed jedes Mal umschalten.
        if (b instanceof StickyKeyBinding) return;
        b.setPressed(pressed);
    }

    @Override public String keyName(int code) {
        if (code == Keys.NONE) return "Keine";
        if (Keys.isMouse(code)) return InputUtil.Type.MOUSE.createFromCode(Keys.mouseButton(code)).getLocalizedText().getString();
        return InputUtil.fromKeyCode(code, 0).getLocalizedText().getString();
    }

    // ---- Kamera ----

    @Override public Perspective perspective() {
        return switch (client.options.getPerspective()) {
            case FIRST_PERSON -> Perspective.FIRST_PERSON;
            case THIRD_PERSON_BACK -> Perspective.THIRD_PERSON_BACK;
            case THIRD_PERSON_FRONT -> Perspective.THIRD_PERSON_FRONT;
        };
    }

    @Override public void setPerspective(Perspective perspective) {
        client.options.setPerspective(switch (perspective) {
            case FIRST_PERSON -> net.minecraft.client.option.Perspective.FIRST_PERSON;
            case THIRD_PERSON_BACK -> net.minecraft.client.option.Perspective.THIRD_PERSON_BACK;
            case THIRD_PERSON_FRONT -> net.minecraft.client.option.Perspective.THIRD_PERSON_FRONT;
        });
    }

    // ---- UI ----

    /**
     * Verzögert über die Task-Queue: Das Öffnen passiert oft mitten in der Tastatur- oder
     * Mausverarbeitung, die sonst denselben Tastendruck noch an den neuen Screen weitergibt.
     */
    @Override public void openScreen(UiScreen screen) {
        client.send(() -> client.setScreen(new AioScreen(screen)));
    }

    @Override public void closeScreen() {
        client.send(() -> client.setScreen(null));
    }

    @Override public boolean isScreenOpen() { return client.currentScreen != null; }

    @Override public boolean isOwnScreenOpen() { return client.currentScreen instanceof AioScreen; }

    @Override public String clipboard() { return client.keyboard.getClipboard(); }

    @Override public void setClipboard(String text) { client.keyboard.setClipboard(text); }

    @Override public void notify(String message) {
        if (client.player == null) {
            AioFabricClient.LOGGER.info(message);
            return;
        }
        client.player.sendMessage(Text.literal("AIO » ").formatted(Formatting.LIGHT_PURPLE)
                .append(Text.literal(message).formatted(Formatting.WHITE)), true);
    }
}
