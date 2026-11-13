package dev.aio.fabric.mixin;

import dev.aio.core.AioCore;
import dev.aio.core.modules.hud.HeldItemInfoModule;
import dev.aio.core.modules.hud.PotionStatusModule;
import dev.aio.core.modules.player.FoodInfoModule;
import dev.aio.fabric.FoodHud;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * HUD layers of this Minecraft version (it has no layer registry yet): hides the vanilla parts an active AIO
 * module replaces and draws the food info directly below/above the hunger and health bars.
 */
@Mixin(Gui.class)
public abstract class HudLayersMixin {
    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    private void aio$hideEffects(CallbackInfo ci) {
        AioCore core = AioCore.get();
        PotionStatusModule potions = core == null ? null : core.module(PotionStatusModule.class);
        if (potions != null && potions.isActive()) ci.cancel();
    }

    @Inject(method = "renderSelectedItemName", at = @At("HEAD"), cancellable = true)
    private void aio$hideItemName(CallbackInfo ci) {
        AioCore core = AioCore.get();
        HeldItemInfoModule info = core == null ? null : core.module(HeldItemInfoModule.class);
        if (info != null && info.replacesVanillaName()) ci.cancel();
    }

    // In this Minecraft version hearts, armour and food are drawn in one method
    @Inject(method = "renderPlayerHealth", at = @At("HEAD"))
    private void aio$foodUnder(GuiGraphics g, CallbackInfo ci) {
        AioCore core = AioCore.get();
        if (core != null) FoodHud.renderUnder(g, core.module(FoodInfoModule.class), g.guiWidth() / 2 + 91, g.guiHeight() - 39);
    }

    @Inject(method = "renderPlayerHealth", at = @At("RETURN"))
    private void aio$foodOver(GuiGraphics g, CallbackInfo ci) {
        AioCore core = AioCore.get();
        if (core != null) FoodHud.renderOver(g, core.module(FoodInfoModule.class), g.guiWidth() / 2 + 91, g.guiHeight() - 39);
    }

    @Inject(method = "renderHearts", at = @At("RETURN"))
    private void aio$healthOver(CallbackInfo ci, @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) GuiGraphics g) {
        AioCore core = AioCore.get();
        if (core != null) FoodHud.renderHealth(g, core.module(FoodInfoModule.class), g.guiWidth() / 2 - 91, g.guiHeight() - 39);
    }
}
