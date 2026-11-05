package dev.aio.fabric.mixin;

import dev.aio.core.AioCore;
import dev.aio.core.event.Events;
import dev.aio.fabric.Freecam;
import dev.aio.fabric.ShieldIndicator;
import dev.aio.fabric.ToolSaverHook;

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
 * Auto Tool / Auto Sword: Events direkt vor dem Abbauen bzw. Schlagen. Ein hier gewählter
 * Hotbar-Slot wird von Vanilla ({@code ensureHasSentCarriedItem}) noch vor der Aktion an den Server
 * geschickt, der Schlag zählt also schon mit der neuen Waffe.
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void aio$startBreak(BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> cir) {
        AioCore core = AioCore.get();
        if (core != null) core.bus().post(new Events.StartBreak(pos.getX(), pos.getY(), pos.getZ()));
        // Tool Saver erst nach Auto Tool: sieht schon das eingewechselte Werkzeug
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

    // ---- Freecam: keine Interaktion aus der Kamera heraus ----

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
