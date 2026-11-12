package dev.aio.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.gui.components.debugchart.ProfilerPieChart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Vanilla draws the pie chart in the F3 screen as soon as data is there. Minecraft delivers it
 * also when only our Profiler Chart needs it: then in the F3 screen only draw if F3+1
 * really on.
 */
@Mixin(DebugScreenOverlay.class)
public abstract class DebugPieChartMixin {
    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/debugchart/ProfilerPieChart;extractRenderState(Lnet/minecraft/client/gui/GuiGraphics;II)V"))
    private void aio$onlyWithF3Chart(ProfilerPieChart chart, GuiGraphics graphics, int w, int h, Operation<Void> original) {
        if (((DebugScreenOverlay) (Object) this).showProfilerChart()) original.call(chart, graphics, w, h);
    }
}
