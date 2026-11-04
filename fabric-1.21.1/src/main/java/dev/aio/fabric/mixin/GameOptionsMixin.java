package dev.aio.fabric.mixin;

import dev.aio.fabric.FabricHooks;
import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Verhindert, dass der Fullbright-Gamma-Wert in options.txt landet. */
@Mixin(GameOptions.class)
public abstract class GameOptionsMixin {
    @Inject(method = "write", at = @At("HEAD"))
    private void aio$beginWrite(CallbackInfo ci) {
        FabricHooks.writingOptions = true;
    }

    @Inject(method = "write", at = @At("RETURN"))
    private void aio$endWrite(CallbackInfo ci) {
        FabricHooks.writingOptions = false;
    }
}
