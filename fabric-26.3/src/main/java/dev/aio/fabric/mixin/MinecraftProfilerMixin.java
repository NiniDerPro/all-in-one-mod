package dev.aio.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.aio.core.AioCore;
import dev.aio.core.modules.performance.ProfilerChartModule;
import dev.aio.fabric.FabricHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Profiler Chart: Minecraft sammelt die Daten fürs Kreisdiagramm nur bei offenem F3 mit F3+1.
 * Nur an dieser einen Stelle (Frame-Schleife in run) wird das zusätzlich eingeschaltet, nicht global,
 * damit die Zahlentasten weiter die Hotbar wechseln statt das Diagramm zu steuern.
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
        FabricHooks.profilerResults = vanilla || ours;
        return vanilla || ours;
    }
}
