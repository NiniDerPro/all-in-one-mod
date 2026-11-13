package dev.aio.forge.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.aio.core.AioCore;
import dev.aio.core.modules.performance.DynamicFpsModule;
import com.mojang.blaze3d.platform.FramerateLimitTracker;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Dynamic FPS: limit the frame rate in the background. */
@Mixin(FramerateLimitTracker.class)
public abstract class MinecraftFpsMixin {
    @ModifyReturnValue(method = "getFramerateLimit", at = @At("RETURN"), require = 0)
    private int aio$dynamicFps(int original) {
        AioCore core = AioCore.get();
        DynamicFpsModule m = core == null ? null : core.module(DynamicFpsModule.class);
        if (m == null) return original;
        int limit = m.limit(Minecraft.getInstance().isWindowActive());
        return limit < 0 ? original : Math.min(original, limit);
    }
}
