package dev.aio.forge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.aio.core.AioCore;
import dev.aio.core.modules.render.BlockOutlineModule;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/** Own block outline: replaces the vanilla line with edges of selectable thickness, colour and fill. */
final class BlockOutlineRenderer {
    private BlockOutlineRenderer() {}

    static void register(AioCore core, Minecraft client) {
        LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register((context, outline) -> {
            BlockOutlineModule module = core.module(BlockOutlineModule.class);
            if (module == null || !module.isActive() || client.level == null) return true;
            BlockPos pos = outline.pos();
            VoxelShape shape = client.level.getBlockState(pos).getShape(client.level, pos);
            if (shape.isEmpty()) return true;

            Vec3 cam = context.levelState().cameraRenderState.pos;
            float t = module.thicknessBlocks();
            int line = module.lineColor();
            List<AABB> edges = new ArrayList<>();
            shape.forAllEdges((x1, y1, z1, x2, y2, z2) -> edges.add(new AABB(
                    Math.min(x1, x2) - t, Math.min(y1, y2) - t, Math.min(z1, z2) - t,
                    Math.max(x1, x2) + t, Math.max(y1, y2) + t, Math.max(z1, z2) + t)));
            List<AABB> fills = module.fill() ? shape.toAabbs() : List.of();

            PoseStack pose = context.poseStack();
            pose.pushPose();
            pose.translate(pos.getX() - cam.x, pos.getY() - cam.y, pos.getZ() - cam.z);
            context.submitNodeCollector().submitCustomGeometry(pose, RenderTypes.debugFilledBox(), (p, buffer) -> {
                for (AABB box : fills) box(p, buffer, box.inflate(0.002), module.fillColor());
                for (AABB box : edges) box(p, buffer, box, line);
            });
            pose.popPose();
            return false; // do not draw the vanilla line
        });
    }

    private static void box(PoseStack.Pose pose, VertexConsumer v, AABB b, int c) {
        float x0 = (float) b.minX, y0 = (float) b.minY, z0 = (float) b.minZ;
        float x1 = (float) b.maxX, y1 = (float) b.maxY, z1 = (float) b.maxZ;
        // vorne, hinten, links, rechts, oben, unten
        quad(pose, v, c, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0);
        quad(pose, v, c, x1, y0, z1, x0, y0, z1, x0, y1, z1, x1, y1, z1);
        quad(pose, v, c, x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1);
        quad(pose, v, c, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0);
        quad(pose, v, c, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);
        quad(pose, v, c, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0);
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer v, int c,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        v.addVertex(pose, ax, ay, az).setColor(c);
        v.addVertex(pose, bx, by, bz).setColor(c);
        v.addVertex(pose, cx, cy, cz).setColor(c);
        v.addVertex(pose, dx, dy, dz).setColor(c);
    }
}
