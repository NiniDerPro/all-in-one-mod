package dev.aio.fabric;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.aio.core.AioCore;
import dev.aio.core.modules.render.BlockOutlineModule;
import net.minecraft.client.Minecraft;
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
        // Drawing hook is not available in this Minecraft version yet (the module is switched off, see AioFabricClient)
    }
}
