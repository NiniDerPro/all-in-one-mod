package dev.aio.fabric;

import dev.aio.core.modules.player.FoodInfoModule;
import dev.aio.core.modules.player.FoodMath;
import dev.aio.fabric.mixin.FoodDataAccessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Locale;
import java.util.Random;

/**
 * Hunger overlays:
 * saturation as golden frames in four levels, exhaustion as a bar behind the hunger bar,
 * a blinking preview for hunger, saturation and hearts when holding food. The icons shake
 * in the same rhythm as the vanilla bar so that everything stays congruent.
 */
public final class FoodHud {
    private FoodHud() {}

    private static final ResourceLocation ICONS = ResourceLocation.fromNamespaceAndPath("aio", "textures/foodinfo/icons.png");
    private static final ResourceLocation FOOD_EMPTY = ResourceLocation.withDefaultNamespace("hud/food_empty");
    private static final ResourceLocation FOOD_HALF = ResourceLocation.withDefaultNamespace("hud/food_half");
    private static final ResourceLocation FOOD_FULL = ResourceLocation.withDefaultNamespace("hud/food_full");
    private static final ResourceLocation FOOD_EMPTY_HUNGER = ResourceLocation.withDefaultNamespace("hud/food_empty_hunger");
    private static final ResourceLocation FOOD_HALF_HUNGER = ResourceLocation.withDefaultNamespace("hud/food_half_hunger");
    private static final ResourceLocation FOOD_FULL_HUNGER = ResourceLocation.withDefaultNamespace("hud/food_full_hunger");
    private static final ResourceLocation HEART_CONTAINER = ResourceLocation.withDefaultNamespace("hud/heart/container");
    private static final ResourceLocation HEART_CONTAINER_HC = ResourceLocation.withDefaultNamespace("hud/heart/container_hardcore");
    private static final ResourceLocation HEART_FULL = ResourceLocation.withDefaultNamespace("hud/heart/full");
    private static final ResourceLocation HEART_FULL_HC = ResourceLocation.withDefaultNamespace("hud/heart/hardcore_full");
    private static final ResourceLocation HEART_HALF = ResourceLocation.withDefaultNamespace("hud/heart/half");
    private static final ResourceLocation HEART_HALF_HC = ResourceLocation.withDefaultNamespace("hud/heart/hardcore_half");

    // ---- Blinking (per client tick) ----
    private static float unclampedFlash, flash;
    private static int flashDir = 1;

    public static void tick() {
        unclampedFlash += flashDir * 0.125F;
        if (unclampedFlash >= 1.5F) flashDir = -1;
        else if (unclampedFlash <= -0.5F) flashDir = 1;
        flash = Math.max(0F, Math.min(1F, unclampedFlash)) * 0.65F;
    }

    private static void resetFlash() {
        unclampedFlash = flash = 0;
        flashDir = 1;
    }

    private static int white(float alpha) { return (Math.round(Math.max(0, Math.min(1, alpha)) * 255) << 24) | 0xFFFFFF; }

    /** Hunger bar visible like vanilla? (survival/adventure, not on a mount with health) */
    static boolean visible(Minecraft mc) {
        return mc.player != null && mc.gameMode != null && mc.gameMode.canHurtPlayer()
                && !(mc.player.getVehicle() instanceof LivingEntity);
    }

    // ---- Shake offset of the icons like in vanilla (same randomness, same rhythm) ----
    private static final int[] FOOD_Y = new int[10];
    private static int[] healthX = new int[0], healthY = new int[0];
    private static int offsetsTick = -1;
    private static final Random RANDOM = new Random();

    private static void offsets(int guiTicks, Player player) {
        if (guiTicks == offsetsTick) return;
        offsetsTick = guiTicks;
        float max = player.getMaxHealth(), absorption = (float) Math.ceil(player.getAbsorptionAmount());
        int hearts = (int) Math.ceil((max + absorption) / 2.0F);
        if (hearts < 0 || hearts > 1000) hearts = 0;
        int rows = (int) Math.ceil(hearts / 10f);
        int rowHeight = Math.max(10 - (rows - 2), 3);
        FoodData fd = player.getFoodData();
        boolean shakeFood = fd.getSaturationLevel() <= 0.0F && guiTicks % (fd.getFoodLevel() * 3 + 1) == 0;
        boolean shakeHealth = Math.ceil(player.getHealth()) <= 4;
        RANDOM.setSeed(guiTicks * 312871L);
        if (healthX.length != hearts) {
            healthX = new int[hearts];
            healthY = new int[hearts];
        }
        for (int i = hearts - 1; i >= 0; --i) {
            int row = (int) Math.ceil((i + 1) / 10f) - 1;
            healthX[i] = i % 10 * 8;
            healthY[i] = -(row * rowHeight) + (shakeHealth ? RANDOM.nextInt(2) : 0);
        }
        for (int i = 0; i < 10; i++) FOOD_Y[i] = shakeFood ? RANDOM.nextInt(3) - 1 : 0;
    }

    private static int foodX(int i) { return -(i * 8) - 9; }

    // ---- Food in hand ----

    private record Held(FoodProperties food, boolean rotten) {}

    private static Held held(Player p) {
        for (ItemStack s : new ItemStack[]{p.getMainHandItem(), p.getOffhandItem()}) {
            FoodProperties f = s.get(DataComponents.FOOD);
            if (f != null && p.canEat(f.canAlwaysEat())) return new Held(f, isRotten(s));
        }
        return null;
    }

    /** Food that causes hunger (vanilla: rotten flesh, raw chicken, pufferfish). */
    static boolean isRotten(ItemStack s) {
        return s.is(Items.ROTTEN_FLESH) || s.is(Items.CHICKEN) || s.is(Items.PUFFERFISH);
    }

    private static float exhaustion(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        if (server == null || mc.player == null) return Float.NaN;
        var sp = server.getPlayerList().getPlayer(mc.player.getUUID());
        return sp == null ? Float.NaN : ((FoodDataAccessor) sp.getFoodData()).aio$exhaustion();
    }

    // ---- Zeichnen ----

    /** Before the hunger bar: exhaustion as a bar. */
    public static void renderUnder(GuiGraphics g, FoodInfoModule m, int right, int top) {
        Minecraft mc = Minecraft.getInstance();
        if (m == null || !m.showExhaustion() || !visible(mc)) return;
        float exhaustion = exhaustion(mc);
        if (!Float.isFinite(exhaustion)) return;
        float ratio = Math.min(1, Math.max(0, exhaustion / FoodMath.MAX_EXHAUSTION));
        int width = (int) (ratio * 81);
        if (width <= 0) return;
        g.blit(RenderType::guiTextured, ICONS, right - width, top, 81 - width, 18, width, 9, 256, 256, white(0.75F));
    }

    /** After the hunger bar: saturation and preview. */
    public static void renderOver(GuiGraphics g, FoodInfoModule m, int right, int top) {
        Minecraft mc = Minecraft.getInstance();
        if (m == null || !m.isActive() || !visible(mc)) return;
        Player p = mc.player;
        int guiTicks = mc.gui.getGuiTicks();
        offsets(guiTicks, p);
        FoodData stats = p.getFoodData();
        if (m.showSaturation()) drawSaturation(g, 0, stats.getSaturationLevel(), right, top, 1F);
        if (m.showValues()) {
            String text = String.format(Locale.ROOT, "Hunger %d · Sat %.1f", stats.getFoodLevel(), stats.getSaturationLevel());
            float ex = exhaustion(mc);
            if (Float.isFinite(ex)) text += String.format(Locale.ROOT, " · Exh %.2f", ex);
            g.drawString(mc.font, text, right - mc.font.width(text), top - 10, 0xFFE0E0E0, true);
        }
        Held held = m.showPreview() ? held(p) : null;
        if (held == null) {
            resetFlash();
            return;
        }
        int nutrition = held.food.nutrition();
        drawHungerPreview(g, nutrition, stats.getFoodLevel(), right, top, flash, held.rotten);
        if (m.showSaturation()) {
            int newFood = stats.getFoodLevel() + nutrition;
            float newSat = stats.getSaturationLevel() + held.food.saturation();
            float gained = newSat > newFood ? newFood - stats.getSaturationLevel() : held.food.saturation();
            drawSaturation(g, gained, stats.getSaturationLevel(), right, top, flash);
        }
    }

    /** After the health bar: hearts that the food brings via natural healing. */
    public static void renderHealth(GuiGraphics g, FoodInfoModule m, int left, int top) {
        Minecraft mc = Minecraft.getInstance();
        if (m == null || !m.showHealthPreview() || !visible(mc)) return;
        Player p = mc.player;
        Held held = held(p);
        if (held == null) return;
        FoodData stats = p.getFoodData();
        // only if the prediction is unambiguous
        if (p.level().getDifficulty() == Difficulty.PEACEFUL || stats.getFoodLevel() >= 18) return;
        if (p.hasEffect(MobEffects.POISON) || p.hasEffect(MobEffects.WITHER) || p.hasEffect(MobEffects.REGENERATION)) return;
        int guiTicks = mc.gui.getGuiTicks();
        offsets(guiTicks, p);
        if (healthX.length == 0) return;
        int newFood = FoodMath.foodAfter(stats.getFoodLevel(), held.food.nutrition());
        float newSat = FoodMath.saturationAfter(stats.getFoodLevel(), stats.getSaturationLevel(), held.food.nutrition(), held.food.saturation());
        float ex = exhaustion(mc);
        float gain = FoodMath.healthIncrement(newFood, newSat, Float.isFinite(ex) ? ex : 0);
        float health = p.getHealth(), modified = Math.min(health + gain, p.getMaxHealth());
        if (modified <= health) return;
        boolean hardcore = p.level().getLevelData().isHardcore();
        int fixed = (int) Math.ceil(modified);
        int start = (int) Math.max(0, Math.ceil(health) / 2.0F), end = (int) Math.max(0, Math.ceil(modified / 2.0F));
        for (int i = start; i < end && i < healthX.length; i++) {
            int x = left + healthX[i], y = top + healthY[i];
            g.blitSprite(RenderType::guiTextured, hardcore ? HEART_CONTAINER_HC : HEART_CONTAINER, x, y, 9, 9, white(flash * 0.25F));
            boolean half = i * 2 + 1 == fixed;
            ResourceLocation icon = half ? (hardcore ? HEART_HALF_HC : HEART_HALF) : (hardcore ? HEART_FULL_HC : HEART_FULL);
            g.blitSprite(RenderType::guiTextured, icon, x, y, 9, 9, white(flash));
        }
    }

    private static void drawSaturation(GuiGraphics g, float gained, float level, int right, int top, float alpha) {
        if (level + gained < 0) return;
        float modified = Math.max(0, Math.min(level + gained, 20));
        int start = gained != 0 ? (int) Math.max(level / 2.0F, 0) : 0;
        int end = (int) Math.ceil(modified / 2.0F);
        for (int i = start; i < end && i < 10; i++) {
            int x = right + foodX(i), y = top + FOOD_Y[i];
            float bar = modified / 2.0F - i;
            int u = bar >= 1 ? 27 : bar > .5 ? 18 : bar > .25 ? 9 : 0;
            g.blit(RenderType::guiTextured, ICONS, x, y, u, 0, 9, 9, 256, 256, white(alpha));
        }
    }

    private static void drawHungerPreview(GuiGraphics g, int restored, int food, int right, int top, float alpha, boolean rotten) {
        if (restored <= 0) return;
        int modified = Math.max(0, Math.min(20, food + restored));
        int start = Math.max(0, food / 2), end = (int) Math.ceil(modified / 2.0F);
        for (int i = start; i < end && i < 10; i++) {
            int x = right + foodX(i), y = top + FOOD_Y[i];
            g.blitSprite(RenderType::guiTextured, rotten ? FOOD_EMPTY_HUNGER : FOOD_EMPTY, x, y, 9, 9, white(alpha * 0.25F));
            boolean half = i * 2 + 1 == modified;
            ResourceLocation icon = half ? (rotten ? FOOD_HALF_HUNGER : FOOD_HALF) : (rotten ? FOOD_FULL_HUNGER : FOOD_FULL);
            g.blitSprite(RenderType::guiTextured, icon, x, y, 9, 9, white(alpha));
        }
    }
}
