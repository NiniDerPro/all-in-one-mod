package dev.aio.fabric.mixin;

import net.minecraft.client.gui.components.debugchart.ProfilerPieChart;
import net.minecraft.util.profiling.ProfileResults;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Gewählte Ebene im F3+1-Diagramm lesen und setzen (Profiler Chart merkt sie sich). */
@Mixin(ProfilerPieChart.class)
public interface ProfilerPieChartAccessor {
    @Accessor("profilerTreePath") String aio$getPath();

    @Accessor("profilerTreePath") void aio$setPath(String path);

    @Accessor("profilerPieChartResults") ProfileResults aio$getResults();
}
