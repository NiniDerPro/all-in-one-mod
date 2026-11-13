package dev.aio.forge.mixin;

import dev.aio.forge.LightOverlayHook;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Light Level Overlay: learns immediately when blocks or light change in a section. */
@Mixin(LevelRenderer.class)
public abstract class LightOverlayDirtyMixin {
    @Inject(method = "setSectionDirty(IIIZ)V", at = @At("HEAD"))
    private void aio$lightOverlayDirty(int sx, int sy, int sz, boolean playerChanged, CallbackInfo ci) {
        LightOverlayHook.sectionDirty(sx, sy, sz);
    }
}
