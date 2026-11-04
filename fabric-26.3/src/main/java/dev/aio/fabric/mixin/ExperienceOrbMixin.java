package dev.aio.fabric.mixin;

import dev.aio.core.AioCore;
import dev.aio.core.modules.performance.XpClumpsModule;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * XP bündeln (nur auf dem eigenen Einzelspieler-Server): Kugeln in der Nähe werden zu einer
 * zusammengefasst (Werte addiert, auch unterschiedliche), und berührte Kugeln werden ohne die
 * Vanilla-Pause sofort aufgesammelt.
 */
@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMixin {
    @Shadow private int count;

    @Shadow public abstract int getValue();

    @Shadow private void setValue(int value) {}

    @Unique
    private static boolean aio$active() {
        AioCore core = AioCore.get();
        XpClumpsModule m = core == null ? null : core.module(XpClumpsModule.class);
        return m != null && m.isActive();
    }

    @Inject(method = "playerTouch", at = @At("HEAD"))
    private void aio$instantPickup(Player player, CallbackInfo ci) {
        if (!player.level().isClientSide() && aio$active()) player.takeXpDelay = 0;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void aio$clump(CallbackInfo ci) {
        ExperienceOrb self = (ExperienceOrb) (Object) this;
        if (self.isRemoved() || !(self.level() instanceof ServerLevel) || (self.tickCount + self.getId()) % 20 != 0) return;
        if (!aio$active()) return;
        List<ExperienceOrb> near = self.level().getEntitiesOfClass(ExperienceOrb.class, self.getBoundingBox().inflate(1.5),
                o -> o != self && !o.isRemoved());
        if (near.isEmpty()) return;
        long total = (long) getValue() * Math.max(1, count);
        for (ExperienceOrb other : near) {
            ExperienceOrbMixin o = (ExperienceOrbMixin) (Object) other;
            total += (long) other.getValue() * Math.max(1, o.count);
            other.discard();
        }
        setValue((int) Math.min(Integer.MAX_VALUE, total));
        count = 1;
    }
}
