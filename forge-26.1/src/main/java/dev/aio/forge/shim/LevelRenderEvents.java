package dev.aio.forge.shim;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;

import java.util.ArrayList;
import java.util.List;

/** Hooks into the level renderer (fired by {@code LevelRenderHooksMixin}): own geometry and the block outline. */
public final class LevelRenderEvents {
    private LevelRenderEvents() {}

    public record Context(LevelRenderState levelState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {}

    public interface CollectSubmits { void collect(Context context); }

    /** Return false to skip the vanilla outline. */
    public interface BeforeBlockOutline { boolean beforeOutline(Context context, BlockOutlineRenderState outline); }

    public static final class Hook<T> {
        public final List<T> list = new ArrayList<>();
        public void register(T listener) { list.add(listener); }
    }

    public static final Hook<CollectSubmits> COLLECT_SUBMITS = new Hook<>();
    public static final Hook<BeforeBlockOutline> BEFORE_BLOCK_OUTLINE = new Hook<>();
}
