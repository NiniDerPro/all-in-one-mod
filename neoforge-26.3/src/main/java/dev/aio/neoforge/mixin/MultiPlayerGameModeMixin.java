package dev.aio.neoforge.mixin;

import dev.aio.core.AioCore;
import dev.aio.core.event.Events;
import dev.aio.neoforge.Freecam;
import dev.aio.neoforge.ShieldIndicator;
import dev.aio.neoforge.ToolSaverHook;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Auto Tool / Auto Sword: events directly before breaking or hitting. A hotbar slot
 * chosen here is sent to the server by vanilla ({@code ensureHasSentCarriedItem}) before the action,
 * so the hit already counts with the new weapon.
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void aio$startBreak(BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> cir) {
        AioCore core = AioCore.get();
        if (core != null) core.bus().post(new Events.StartBreak(pos.getX(), pos.getY(), pos.getZ()));
        // Tool Saver only after Auto Tool: already sees the swapped-in tool
        var p = Minecraft.getInstance().player;
        if (p != null && ToolSaverHook.block(p.getMainHandItem())) cir.setReturnValue(false);
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void aio$attack(Player player, Entity target, CallbackInfo ci) {
        AioCore core = AioCore.get();
        if (core != null) core.bus().post(new Events.AttackEntity(target instanceof LivingEntity));
        if (ToolSaverHook.block(player.getMainHandItem())) {
            ci.cancel();
            return;
        }
        ShieldIndicator.onAttack(player, target);
    }

    // ---- Freecam: no interaction from the camera ----

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void aio$freecamBreak(BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> cir) {
        if (Freecam.active() && Freecam.preventInteractions()) cir.setReturnValue(false);
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void aio$freecamContinue(BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> cir) {
        if (Freecam.active() && Freecam.preventInteractions()) cir.setReturnValue(false);
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void aio$freecamUseOn(LocalPlayer player, InteractionHand hand,
                                  BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (Freecam.active() && Freecam.preventInteractions()) cir.setReturnValue(InteractionResult.FAIL);
    }

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void aio$freecamUse(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (Freecam.active() && Freecam.preventInteractions()) cir.setReturnValue(InteractionResult.FAIL);
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void aio$freecamAttack(Player player, Entity target, CallbackInfo ci) {
        if (Freecam.active() && Freecam.preventInteractions()) ci.cancel();
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void aio$freecamInteract(Player player, Entity target, EntityHitResult hit,
                                     InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (Freecam.active() && Freecam.preventInteractions()) cir.setReturnValue(InteractionResult.FAIL);
    }
}
