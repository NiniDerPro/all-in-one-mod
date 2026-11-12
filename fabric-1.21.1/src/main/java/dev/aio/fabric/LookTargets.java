package dev.aio.fabric;

import dev.aio.core.platform.ItemRef;
import dev.aio.core.platform.LookTarget;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** What the player is currently looking at, prepared for the block info. */
final class LookTargets {
    private final Minecraft client;

    LookTargets(Minecraft client) {
        this.client = client;
    }

    private final Map<String, String> modNames = new ConcurrentHashMap<>();

    /** Display name of a mod for a namespace ("minecraft" -> "Minecraft"). */
    private String modName(String namespace) {
        return modNames.computeIfAbsent(namespace, ns -> FabricLoader.getInstance().getModContainer(ns)
                .map(c -> c.getMetadata().getName())
                .orElse(ns.isEmpty() ? ns : Character.toUpperCase(ns.charAt(0)) + ns.substring(1)));
    }

    LookTarget find(double maxDistance) {
        LocalPlayer p = client.player;
        HitResult hit = client.hitResult;
        if (p != null && maxDistance > 0 && (hit == null || hit.getType() == HitResult.Type.MISS)) {
            // look farther than the reach (blocks only, like binoculars for the look-at info)
            hit = p.pick(maxDistance, client.getTimer().getGameTimeDeltaPartialTick(false), false);
        }
        if (p == null || client.level == null || hit == null || hit.getType() == HitResult.Type.MISS) return null;

        if (hit instanceof EntityHitResult eh) {
            Entity e = eh.getEntity();
            String source = modName(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getNamespace());
            ItemRef icon = e instanceof ItemEntity item ? FabricItemRef.of(item.getItem()) : ItemRef.EMPTY;
            BlockPos pos = e.blockPosition();
            String type = e.getType().getDescription().getString();
            if (e instanceof LivingEntity living) {
                return LookTarget.entity(e.getDisplayName().getString(), type, source, icon,
                        living.getHealth(), living.getMaxHealth(), living.getArmorValue(), pos.getX(), pos.getY(), pos.getZ());
            }
            return LookTarget.entity(e.getDisplayName().getString(), type, source, icon, Float.NaN, Float.NaN, -1,
                    pos.getX(), pos.getY(), pos.getZ());
        }

        if (hit instanceof BlockHitResult bh) {
            BlockPos pos = bh.getBlockPos();
            BlockState state = client.level.getBlockState(pos);
            if (state.isAir()) return null;
            String source = modName(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace());
            ItemRef icon = FabricItemRef.of(state.getBlock().getCloneItemStack(client.level, pos, state));

            String tool = toolType(state);
            Boolean correct = null;
            if (state.requiresCorrectToolForDrops()) {
                correct = p.hasCorrectToolForDrops(state); // no drop without a matching tool
            } else if (tool != null && p.getMainHandItem().getDestroySpeed(state) > 1f) {
                correct = Boolean.TRUE; // matching tool in hand (it can be broken without one too)
            }

            return LookTarget.block(state.getBlock().getName().getString(), source, icon, correct, tool, growth(state),
                    pos.getX(), pos.getY(), pos.getZ(), BlockDetails.of(client, state, pos)).withToolIcon(toolIcon(state));
        }
        return null;
    }

    private static final String[] TOOL_TIERS = {"wooden", "stone", "copper", "iron", "golden", "diamond", "netherite"};
    private final Map<BlockState, ItemRef> toolIconCache = new HashMap<>();

    /**
     * Tool icon for the look-at info: the weakest tool of the matching kind with which the block
     * drops (stone → wooden pickaxe, diamond ore → iron pickaxe). Without a tool kind: no icon.
     */
    private ItemRef toolIcon(BlockState state) {
        ItemRef cached = toolIconCache.get(state);
        if (cached != null) return cached;
        String kind = state.is(BlockTags.MINEABLE_WITH_PICKAXE) ? "pickaxe" : state.is(BlockTags.MINEABLE_WITH_AXE) ? "axe"
                : state.is(BlockTags.MINEABLE_WITH_SHOVEL) ? "shovel" : state.is(BlockTags.MINEABLE_WITH_HOE) ? "hoe" : null;
        ItemRef result = ItemRef.EMPTY;
        if (kind != null) {
            for (String tier : TOOL_TIERS) {
                var item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.withDefaultNamespace(tier + "_" + kind));
                if (item.isEmpty()) continue;
                ItemStack stack = new ItemStack(item.get());
                if (!state.requiresCorrectToolForDrops() || stack.isCorrectToolForDrops(state)) {
                    result = FabricItemRef.of(stack);
                    break;
                }
            }
        }
        if (toolIconCache.size() > 512) toolIconCache.clear();
        toolIconCache.put(state, result);
        return result;
    }

    /** Growth 0..1, only for plants that really grow (not sugar cane, cactus, fire, ...). */
    private static float growth(BlockState state) {
        var block = state.getBlock();
        if (block instanceof CropBlock crop) {
            return crop.getMaxAge() > 0 ? crop.getAge(state) / (float) crop.getMaxAge() : Float.NaN;
        }
        if (block instanceof NetherWartBlock || block instanceof SweetBerryBushBlock) return age(state, BlockStateProperties.AGE_3, 3);
        if (block instanceof CocoaBlock) return age(state, BlockStateProperties.AGE_2, 2);
        if (block instanceof StemBlock) return age(state, BlockStateProperties.AGE_7, 7);
        if (block instanceof SaplingBlock) return age(state, BlockStateProperties.STAGE, 1);
        return Float.NaN;
    }

    private static float age(BlockState state, IntegerProperty property, int max) {
        return state.hasProperty(property) ? state.getValue(property) / (float) max : Float.NaN;
    }

    private static String toolType(BlockState state) {
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) return "Pickaxe";
        if (state.is(BlockTags.MINEABLE_WITH_AXE)) return "Axe";
        if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) return "Shovel";
        if (state.is(BlockTags.MINEABLE_WITH_HOE)) return "Hoe";
        return null;
    }
}
