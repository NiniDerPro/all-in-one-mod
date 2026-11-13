package dev.aio.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.aio.core.AioCore;
import dev.aio.core.modules.performance.ProfilerChartModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.profiling.ProfileResults;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Profiler chart: in this Minecraft version the game draws the chart itself at the end of the frame (bottom
 * right). It is drawn when F3+1 asks for it or while the Profiler Chart module is on and the HUD is visible.
 */
@Mixin(Minecraft.class)
public abstract class DebugPieChartMixin {
    @WrapOperation(method = "runTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;renderFpsMeter(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/util/profiling/ProfileResults;)V"))
    private void aio$chart(Minecraft mc, GuiGraphics graphics, ProfileResults results, Operation<Void> original) {
        if (mc.options.renderDebugCharts) {
            original.call(mc, graphics, results);
            return;
        }
        AioCore core = AioCore.get();
        ProfilerChartModule m = core == null ? null : core.module(ProfilerChartModule.class);
        if (m != null && m.collecting() && !mc.options.hideGui && mc.screen == null) original.call(mc, graphics, results);
    }
}
