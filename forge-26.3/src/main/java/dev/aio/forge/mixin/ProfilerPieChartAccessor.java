package dev.aio.forge.mixin;

import net.minecraft.client.gui.components.debugchart.ProfilerPieChart;
import net.minecraft.util.profiling.ProfileResults;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read and set the selected level in the F3+1 chart (Profiler Chart remembers it). */
@Mixin(ProfilerPieChart.class)
public interface ProfilerPieChartAccessor {
    @Accessor("profilerTreePath") String aio$getPath();

    @Accessor("profilerTreePath") void aio$setPath(String path);

    @Accessor("profilerPieChartResults") ProfileResults aio$getResults();
}
