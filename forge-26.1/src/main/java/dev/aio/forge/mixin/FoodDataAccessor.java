package dev.aio.forge.mixin;

import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read exhaustion (has no public getter). */
@Mixin(FoodData.class)
public interface FoodDataAccessor {
    @Accessor("exhaustionLevel")
    float aio$exhaustion();
}
