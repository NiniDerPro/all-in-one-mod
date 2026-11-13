package dev.aio.neoforge.freecam;

import com.mojang.authlib.GameProfile;
import dev.aio.neoforge.Freecam;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoveSimulationType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec2;

import java.util.UUID;

/**
 * The camera is an own, invisible client player that is registered in the world and
 * therefore ticks like a player, moves and can be rendered.
 * Adapted from Freecam (MIT), see NOTICE.md.
 */
public class FreeCamera extends AbstractClientPlayer {
    private static final Minecraft MC = Minecraft.getInstance();

    private final KeyboardInput keyboardInput;

    private float yBob;
    private float xBob;
    private float yBobO;
    private float xBobO;

    public FreeCamera(int id) {
        super(MC.level, new GameProfile(UUID.randomUUID(), "FreeCamera"));
        setId(id);
        setPose(Pose.SWIMMING);
        getAbilities().flying = true;
        keyboardInput = new KeyboardInput(MC.options);
    }

    @Override
    public void tick() {
        getInput().tick();
        doMotion();
        super.tick();
    }

    @Override
    public void copyPosition(Entity entity) {
        applyPosition(new FreecamPosition(entity));
    }

    public void applyPosition(FreecamPosition position) {
        snapTo(position.x, position.y, position.z, position.yaw, position.pitch);
        xBob = getXRot();
        yBob = getYRot();
        xBobO = xBob; // prevents the camera from turning when switching on
        yBobO = yBob;
    }

    private ClientLevel getClientLevel() {
        return (ClientLevel) level();
    }

    public void spawn() {
        getClientLevel().addEntity(this);
    }

    public void despawn() {
        getClientLevel().removeEntity(getId(), RemovalReason.DISCARDED);
    }

    // No fall damage sound on landing
    @Override
    protected void checkFallDamage(double heightDifference, boolean onGround, BlockState landedState, BlockPos landedPosition) {}

    // Hit and use animation of the hand come from the real player
    @Override
    public SwingDescription getCurrentSwing() {
        return MC.player.getCurrentSwing();
    }

    @Override
    public float getSwingAnimation(final float partialTicks) {
        return MC.player.getSwingAnimation(partialTicks);
    }

    @Override
    public boolean isSwinging() {
        return MC.player.isSwinging();
    }

    @Override
    public int getUseItemRemainingTicks() {
        return MC.player.getUseItemRemainingTicks();
    }

    @Override
    public boolean isUsingItem() {
        return MC.player.isUsingItem();
    }

    // Do not let ladders/vines or water slow it down
    @Override
    public boolean onClimbable() {
        return false;
    }

    @Override
    public boolean isInWater() {
        return false;
    }

    // The player's night vision also applies to the camera
    @Override
    public MobEffectInstance getEffect(Holder<MobEffect> effect) {
        return MC.player.getEffect(effect);
    }

    // Pistons do not push the camera when it flies through blocks
    @Override
    public PushReaction getPistonPushReaction() {
        if (Freecam.noClip()) return PushReaction.IGNORE_ENTITY;
        return super.getPistonPushReaction();
    }

    // No collision with solid entities (shulkers, boats)
    @Override
    public boolean canCollideWith(Entity other) {
        return false;
    }

    // Immer in Schwimmhaltung (kleine Hitbox)
    @Override
    public void setPose(Pose pose) {
        super.setPose(Pose.SWIMMING);
    }

    // No underwater sounds
    @Override
    protected boolean updateIsUnderwater() {
        this.wasUnderwater = this.isEyeInFluid(FluidTags.WATER);
        return this.wasUnderwater;
    }

    @Override
    protected void doWaterSplashEffect() {}

    private void doMotion() {
        getAbilities().setFlyingSpeed(0);
        Motion.doMotion(this, Freecam.horizontalSpeed(), Freecam.verticalSpeed());
        getAbilities().flying = true;
        setOnGround(false);
    }

    @Override
    public float getViewXRot(float partialTick) {
        return this.getXRot();
    }

    @Override
    public float getViewYRot(float partialTick) {
        return this.getYRot();
    }

    @Override
    public boolean isEffectiveAi() {
        return true;
    }

    @Override
    public MoveSimulationType getMoveSimulationType() {
        return MoveSimulationType.SERVER_AND_CLIENT;
    }

    @Override
    protected void applyInput() {
        Vec2 moveVector = getInput().getMoveVector();
        if (moveVector.lengthSquared() != 0.0F) moveVector = moveVector.scale(0.98F);
        this.xxa = moveVector.x;
        this.zza = moveVector.y;
        this.jumping = isJumpKeyDown();
        this.setSprinting((MC.options.keySprint.isDown() && this.isForwardKeyDown()) || (this.isForwardKeyDown() && this.isSprinting()));
        this.yBobO = this.yBob;
        this.xBobO = this.xBob;
        this.xBob = this.xBob + (this.getXRot() - this.xBob) * 0.5F;
        this.yBob = this.yBob + (this.getYRot() - this.yBob) * 0.5F;
    }

    private ClientInput getInput() {
        return keyboardInput;
    }

    public boolean isForwardKeyDown() { return getInput().keyPresses.forward(); }

    public boolean isBackwardKeyDown() { return getInput().keyPresses.backward(); }

    public boolean isRightKeyDown() { return getInput().keyPresses.right(); }

    public boolean isLeftKeyDown() { return getInput().keyPresses.left(); }

    public boolean isJumpKeyDown() { return getInput().keyPresses.jump(); }

    public boolean isSneakKeyDown() { return getInput().keyPresses.shift(); }
}
