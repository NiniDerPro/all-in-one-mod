package dev.aio.fabric.mixin;

import dev.aio.fabric.ToolSaverHook;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tool Saver: fast kaputte Werkzeuge/Waffen nicht mehr benutzen (Weiterabbauen und Benutzen).
 * Abbau-Start und Schlag prüft {@link MultiPlayerGameModeMixin} direkt nach Auto Tool / Auto Weapon.
 */
@Mixin(value = MultiPlayerGameMode.class, priority = 1100)
public abstract class ToolSaverMixin {
    @Unique
    private static boolean aio$block(ItemStack stack) { return ToolSaverHook.block(stack); }

    @Unique
    private static ItemStack aio$main() {
        var p = Minecraft.getInstance().player;
        return p == null ? ItemStack.EMPTY : p.getMainHandItem();
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void aio$toolSaverContinue(BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> cir) {
        if (aio$block(aio$main())) cir.setReturnValue(false);
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void aio$toolSaverUseOn(LocalPlayer player, InteractionHand hand, BlockHitResult hit,
                                    CallbackInfoReturnable<InteractionResult> cir) {
        if (aio$block(player.getItemInHand(hand))) cir.setReturnValue(InteractionResult.FAIL);
    }

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void aio$toolSaverUse(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (aio$block(player.getItemInHand(hand))) cir.setReturnValue(InteractionResult.FAIL);
    }
}
