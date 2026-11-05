package dev.aio.fabric.mixin;

import dev.aio.core.AioCore;
import dev.aio.core.modules.player.ToolSaverModule;
import dev.aio.fabric.FabricItemRef;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tool Saver: fast kaputte Werkzeuge/Waffen nicht mehr benutzen. Läuft nach den Auto-Tool-
 * Injektionen (eigene Mixin-Klasse mit späterer Priorität), sieht also schon das eingewechselte Item.
 */
@Mixin(value = MultiPlayerGameMode.class, priority = 1100)
public abstract class ToolSaverMixin {
    @Unique
    private static boolean aio$block(ItemStack stack) {
        AioCore core = AioCore.get();
        ToolSaverModule m = core == null ? null : core.module(ToolSaverModule.class);
        if (m == null || stack == null || stack.isEmpty()) return false;
        dev.aio.core.platform.ItemRef ref = FabricItemRef.of(stack);
        if (!m.shouldBlock(ref)) return false;
        m.onBlocked(ref);
        return true;
    }

    @Unique
    private static ItemStack aio$main() {
        var p = Minecraft.getInstance().player;
        return p == null ? ItemStack.EMPTY : p.getMainHandItem();
    }

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void aio$toolSaverStart(BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> cir) {
        if (aio$block(aio$main())) cir.setReturnValue(false);
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void aio$toolSaverContinue(BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> cir) {
        if (aio$block(aio$main())) cir.setReturnValue(false);
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void aio$toolSaverAttack(Player player, Entity target, CallbackInfo ci) {
        if (aio$block(player.getMainHandItem())) ci.cancel();
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void aio$toolSaverUseOn(net.minecraft.client.player.LocalPlayer player, InteractionHand hand, BlockHitResult hit,
                                    CallbackInfoReturnable<InteractionResult> cir) {
        if (aio$block(player.getItemInHand(hand))) cir.setReturnValue(InteractionResult.FAIL);
    }

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void aio$toolSaverUse(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (aio$block(player.getItemInHand(hand))) cir.setReturnValue(InteractionResult.FAIL);
    }
}
