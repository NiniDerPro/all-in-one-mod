package dev.aio.fabric.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.aio.fabric.Freecam;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Adapted from Freecam (MIT), see dev/aio/fabric/freecam/NOTICE.md
@Mixin(Hud.class)
public class FreecamHudMixin {
    /** Hearts, hunger, hotbar etc. keep showing the real player. */
    @ModifyReturnValue(method = "getCameraPlayer", at = @At("RETURN"))
    private Player aio$freecamHudPlayer(Player original) {
        return Freecam.active() ? Minecraft.getInstance().player : original;
    }
}
