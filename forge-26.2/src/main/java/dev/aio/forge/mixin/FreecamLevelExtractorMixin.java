package dev.aio.forge.mixin;

import dev.aio.forge.Freecam;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Adapted from Freecam (MIT), see dev/aio/fabric/freecam/NOTICE.md
@Mixin(LevelExtractor.class)
public abstract class FreecamLevelExtractorMixin {
    @Shadow protected abstract EntityRenderState extractEntity(Entity e, float g);

    /** The own player would otherwise not be drawn when it is not the camera. */
    @Inject(method = "extractVisibleEntities", at = @At("RETURN"))
    private void aio$freecamShowPlayer(Camera camera, Frustum frustum, DeltaTracker deltaTracker, LevelRenderState renderState, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null && Freecam.active() && Freecam.showPlayer()) {
            Entity player = mc.player;
            TickRateManager tickRateManager = mc.level.tickRateManager();
            float g = deltaTracker.getGameTimeDeltaPartialTick(!tickRateManager.isEntityFrozen(player));
            renderState.entityRenderStates.add(this.extractEntity(player, g));
        }
    }
}
