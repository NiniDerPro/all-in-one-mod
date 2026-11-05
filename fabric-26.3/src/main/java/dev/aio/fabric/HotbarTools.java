package dev.aio.fabric;


import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Bestes Werkzeug bzw. beste Waffe in der Hotbar (Auto Tool, Auto Weapon); schont vom Tool Saver geschützte Items. */
final class HotbarTools {
    private final Minecraft client;

    HotbarTools(Minecraft client) {
        this.client = client;
    }

    int bestToolSlot() {
        LocalPlayer p = client.player;
        if (p == null || client.level == null || !(client.hitResult instanceof BlockHitResult bh)) return -1;
        BlockState state = client.level.getBlockState(bh.getBlockPos());
        if (state.isAir()) return -1;
        int current = p.getInventory().getSelectedSlot();
        int best = -1;
        float bestScore = toolScore(p.getInventory().getItem(current), state);
        for (int i = 0; i < 9; i++) {
            float score = toolScore(p.getInventory().getItem(i), state);
            if (score > bestScore + 0.01f) {
                bestScore = score;
                best = i;
            }
        }
        return best;
    }

    private static float toolScore(ItemStack stack, BlockState state) {
        float speed = stack.getDestroySpeed(state);
        if (state.requiresCorrectToolForDrops() && stack.isCorrectToolForDrops(state)) speed += 1000;
        // fast kaputte Werkzeuge schonen
        if (stack.isDamageableItem() && stack.getMaxDamage() - stack.getDamageValue() <= 2) speed -= 2000;
        return speed;
    }

    int bestWeaponSlot(boolean preferAxe) {
        LocalPlayer p = client.player;
        if (p == null) return -1;
        int best = -1;
        int bestScore = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack s = p.getInventory().getItem(i);
            int type = s.is(ItemTags.SWORDS) ? (preferAxe ? 1 : 2) : s.is(ItemTags.AXES) ? (preferAxe ? 2 : 1) : 0;
            if (type == 0) continue; // geschützte Waffen nicht meiden: Tool Saver blockiert statt zu wechseln
            // höhere Stufe = mehr Haltbarkeit (Holz < Stein < Eisen < Diamant < Netherit)
            int score = type * 100_000 + s.getMaxDamage();
            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
        }
        return best;
    }
}
