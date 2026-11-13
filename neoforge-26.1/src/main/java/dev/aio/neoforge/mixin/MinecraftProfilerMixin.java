package dev.aio.neoforge.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.aio.core.AioCore;
import dev.aio.core.modules.performance.ProfilerChartModule;
import dev.aio.neoforge.NeoForgeHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Profiler Chart: Minecraft only collects the data for the pie chart with F3 open and F3+1.
 * Only at this one place (frame loop in run) it is additionally switched on, not globally,
 * so that the number keys keep switching the hotbar instead of controlling the chart.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftProfilerMixin {
    @WrapOperation(method = "run", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/DebugScreenOverlay;showProfilerChart()Z"))
    private boolean aio$profilerChart(DebugScreenOverlay overlay, Operation<Boolean> original) {
        boolean vanilla = original.call(overlay);
        AioCore core = AioCore.get();
        ProfilerChartModule m = core == null ? null : core.module(ProfilerChartModule.class);
        boolean ours = m != null && m.collecting();
        NeoForgeHooks.profilerResults = vanilla || ours;
        return vanilla || ours;
    }
}
