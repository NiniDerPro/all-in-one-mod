package dev.aio.forge;

import com.mojang.brigadier.arguments.StringArgumentType;

import dev.aio.core.AioCore;
import dev.aio.core.i18n.Lang;
import dev.aio.core.modules.navigation.SharedWaypoint;
import dev.aio.core.modules.navigation.WaypointsModule;

import net.minecraft.commands.Commands;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.List;

/**
 * Shared waypoints in chat: the raw line ("xaero-waypoint:Base:B:100:64:-200:...") becomes
 * "Steve shared a waypoint: [Base 100, 64, -200]"; a click on the part in brackets creates the
 * waypoint. The click runs via a client command that never goes to the server.
 */
final class ChatWaypoints {
    private ChatWaypoints() {}

    static final String COMMAND = "aiowaypoint";

    static void register(AioCore core) {
        RegisterClientCommandsEvent.BUS.addListener(event ->
                event.getDispatcher().register(Commands.literal(COMMAND).then(
                        Commands.argument("data", StringArgumentType.greedyString()).executes(c -> {
                            var f = SharedWaypoint.Found.fromPayload(StringArgumentType.getString(c, "data"));
                            WaypointsModule wp = core.module(WaypointsModule.class);
                            if (f != null && wp != null) wp.addShared(f);
                            return 1;
                        }))));
    }

    /** Rewrites the message if it contains a shared waypoint; otherwise unchanged. */
    static Component rewrite(AioCore core, Component message) {
        String text = message.getString();
        List<SharedWaypoint.Match> found = SharedWaypoint.find(text);
        if (found.isEmpty()) return message;
        SharedWaypoint.Match m = found.get(0);
        String sender = text.substring(0, m.start()).replaceAll("[<>\\[\\]:]", "").trim();
        var f = m.waypoint();
        String where = f.x() + ", " + (f.y() == null ? "~" : f.y()) + ", " + f.z();
        MutableComponent out = Component.literal(sender.isEmpty() ? "" : sender + " ").withStyle(ChatFormatting.WHITE);
        out.append(Component.literal(Lang.tr("aio.ui.waypoints.shared_a", "shared a waypoint: ")).withStyle(ChatFormatting.GRAY));
        Style click = Style.EMPTY.withColor(f.color()).withUnderlined(true)
                .withClickEvent(new ClickEvent.RunCommand("/" + COMMAND + " " + f.payload()))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal(Lang.tr("aio.ui.waypoints.click_add", "Click to add this waypoint"))));
        out.append(Component.literal("[" + f.name() + "  " + where + "]").withStyle(click));
        String rest = text.substring(m.end()).trim();
        if (!rest.isEmpty()) out.append(Component.literal(" " + rest).withStyle(ChatFormatting.GRAY));
        return out;
    }
}
