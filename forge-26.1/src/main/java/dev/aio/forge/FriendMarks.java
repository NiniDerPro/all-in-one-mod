package dev.aio.forge;

import dev.aio.core.AioCore;
import dev.aio.core.modules.render.FriendsModule;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/** Friends: colour per player, worked out at most once per tick and player. */
public final class FriendMarks {
    private FriendMarks() {}

    private static final Map<Integer, Integer> COLORS = new HashMap<>();
    private static long tick = Long.MIN_VALUE;

    /** Active module or null. */
    public static FriendsModule module() {
        AioCore core = AioCore.get();
        FriendsModule m = core == null ? null : core.module(FriendsModule.class);
        return m != null && m.isActive() ? m : null;
    }

    /** Rule colour (ARGB) for an entity or {@link FriendsModule#NONE}. The own player is never marked. */
    public static int color(FriendsModule m, Entity e) {
        if (!(e instanceof Player p)) return FriendsModule.NONE;
        Minecraft mc = Minecraft.getInstance();
        // only players of the client world (not the integrated server's copies); the own player is never marked
        if (p == mc.player || mc.level == null || p.level() != mc.level) return FriendsModule.NONE;
        long now = mc.level.getGameTime();
        if (now != tick) {
            COLORS.clear();
            tick = now;
        }
        Integer cached = COLORS.get(p.getId());
        if (cached != null) return cached;
        String tab = null;
        PlayerInfo info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(p.getUUID());
        if (info != null) {
            Component c = info.getTabListDisplayName();
            if (c != null) tab = c.getString();
        }
        int color = m.colorFor(p.getGameProfile().name(), p.getDisplayName().getString(), tab);
        COLORS.put(p.getId(), color);
        return color;
    }

    /** Multiplies two ARGB colours channel by channel (tint on top of the existing colour). */
    public static int multiply(int a, int b) {
        int alpha = (a >>> 24) * (b >>> 24) / 255;
        int r = ((a >> 16) & 0xFF) * ((b >> 16) & 0xFF) / 255;
        int g = ((a >> 8) & 0xFF) * ((b >> 8) & 0xFF) / 255;
        int bl = (a & 0xFF) * (b & 0xFF) / 255;
        return alpha << 24 | r << 16 | g << 8 | bl;
    }
}
