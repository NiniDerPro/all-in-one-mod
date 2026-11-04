package dev.aio.fabric.mixin;

import dev.aio.fabric.Freecam;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Freecam: Der Spieler bekommt keine Bewegung, die Tasten steuern die Kamera. */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void aio$freecam(CallbackInfo ci) {
        if (!Freecam.active()) return;
        ClientInput input = (ClientInput) (Object) this;
        input.keyPresses = Input.EMPTY;
        ((ClientInputAccessor) input).aio$setMoveVector(Vec2.ZERO);
    }
}
