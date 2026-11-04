package dev.aio.fabric.mixin;

import dev.aio.core.AioCore;
import dev.aio.core.modules.render.FullbrightModule;
import dev.aio.fabric.FabricHooks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fullbright: liefert für die Gamma-Option einen höheren Wert, ohne ihn zu speichern.
 * Beim Schreiben von options.txt ({@link FabricHooks#writingOptions}) bleibt der echte Wert stehen.
 */
@Mixin(SimpleOption.class)
public abstract class SimpleOptionMixin<T> {
    @SuppressWarnings("unchecked")
    @Inject(method = "getValue", at = @At("HEAD"), cancellable = true)
    private void aio$fullbright(CallbackInfoReturnable<T> cir) {
        if (FabricHooks.writingOptions) return;
        AioCore core = AioCore.get();
        if (core == null) return;
        FullbrightModule fullbright = core.module(FullbrightModule.class);
        if (fullbright == null || !fullbright.isActive()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null || (Object) this != client.options.getGamma()) return;
        cir.setReturnValue((T) (Object) fullbright.gammaOverride());
    }
}
