package dev.aio.fabric.mixin;

import dev.aio.core.AioCore;
import dev.aio.core.event.Events;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
    @Inject(method = "startDestroyBlock", at = @At("HEAD"))
    private void aio$startBreak(BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> cir) {
        AioCore core = AioCore.get();
        if (core != null) core.bus().post(new Events.StartBreak(pos.getX(), pos.getY(), pos.getZ()));
    }

    @Inject(method = "attack", at = @At("HEAD"))
    private void aio$attack(Player player, Entity target, CallbackInfo ci) {
        AioCore core = AioCore.get();
        if (core != null) core.bus().post(new Events.AttackEntity(target instanceof LivingEntity));
    }
}
