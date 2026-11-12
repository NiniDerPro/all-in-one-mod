package dev.aio.fabric;

import dev.aio.core.modules.info.VillagerInfoModule;
import dev.aio.core.platform.WorldLabel;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remembers the trade offers of villagers once their trade window is opened
 * (before that the client does not know them), for the display above their heads.
 */
final class VillagerOffers {
    private VillagerOffers() {}

    private static final Map<UUID, List<WorldLabel.Line>> OFFERS = new ConcurrentHashMap<>();

    static void clear() { OFFERS.clear(); }

    /** Take over the offers of a villager (trade window open, offers arrived). */
    static void remember(Entity villager, MerchantOffers offers) {
        if (villager == null || offers == null || offers.isEmpty()) return;
        List<WorldLabel.Line> lines = new ArrayList<>();
        for (MerchantOffer offer : offers) {
            ItemStack result = offer.getResult();
            ItemStack cost = offer.getCostA();
            String price = cost.getCount() + " " + cost.getHoverName().getString();
            boolean soldOut = offer.isOutOfStock();
            ItemEnchantments stored = result.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
            if (!stored.isEmpty()) {
                for (Object2IntMap.Entry<Holder<Enchantment>> e : stored.entrySet()) {
                    boolean curse = e.getKey().is(EnchantmentTags.CURSE);
                    String name = Enchantment.getFullname(e.getKey(), e.getIntValue()).getString();
                    int color = soldOut ? VillagerInfoModule.SOLD_OUT_COLOR : curse ? 0xFFFF5555 : 0xFFFFFF55;
                    lines.add(new WorldLabel.Line(name + " · " + price, color, true));
                }
            } else {
                int color = soldOut ? VillagerInfoModule.SOLD_OUT_COLOR : 0xFFFFFFFF;
                lines.add(new WorldLabel.Line(result.getCount() + "× " + result.getHoverName().getString() + " · " + price, color, false));
            }
        }
        OFFERS.put(villager.getUUID(), lines);
    }

    static List<WorldLabel> labels(Minecraft client, double maxDistance, float partialTick) {
        if (OFFERS.isEmpty() || client.level == null || client.player == null) return List.of();
        List<WorldLabel> out = new ArrayList<>();
        Vec3 eye = client.player.getEyePosition(partialTick);
        double max2 = maxDistance * maxDistance;
        for (Entity e : client.level.entitiesForRendering()) {
            if (!(e instanceof AbstractVillager)) continue;
            List<WorldLabel.Line> lines = OFFERS.get(e.getUUID());
            if (lines == null) continue;
            Vec3 pos = e.getPosition(partialTick).add(0, e.getBbHeight() + 0.55, 0);
            double d2 = pos.distanceToSqr(eye);
            if (d2 > max2) continue;
            out.add(new WorldLabel(pos.x, pos.y, pos.z, Math.sqrt(d2), lines));
        }
        return out;
    }
}
