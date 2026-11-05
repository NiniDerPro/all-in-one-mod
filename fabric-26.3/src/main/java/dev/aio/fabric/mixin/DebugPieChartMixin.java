package dev.aio.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.gui.components.debugchart.ProfilerPieChart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Vanilla zeichnet im F3-Bildschirm das Kreisdiagramm, sobald Daten da sind. Die liefert Minecraft
 * auch, wenn nur unser Profiler Chart sie braucht: dann im F3-Bildschirm nur zeichnen, wenn F3+1
 * wirklich an ist.
 */
@Mixin(DebugScreenOverlay.class)
public abstract class DebugPieChartMixin {
    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/debugchart/ProfilerPieChart;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V"))
    private void aio$onlyWithF3Chart(ProfilerPieChart chart, GuiGraphicsExtractor graphics, int w, int h, Operation<Void> original) {
        if (((DebugScreenOverlay) (Object) this).showProfilerChart()) original.call(chart, graphics, w, h);
    }
}
