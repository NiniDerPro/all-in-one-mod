package dev.aio.fabric.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.util.profiling.ProfileResults;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read and set the selected level in the F3+1 chart (Profiler Chart remembers it). */
@Mixin(Minecraft.class)
public interface ProfilerPieChartAccessor {
    @Accessor("debugPath") String aio$getPath();
    @Accessor("debugPath") void aio$setPath(String path);
    @Accessor("fpsPieResults") ProfileResults aio$getResults();
}
