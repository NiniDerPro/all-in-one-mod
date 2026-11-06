package dev.aio.fabric;

import com.mojang.brigadier.arguments.StringArgumentType;

import dev.aio.core.AioCore;
import dev.aio.core.i18n.Lang;
import dev.aio.core.modules.navigation.SharedWaypoint;
import dev.aio.core.modules.navigation.WaypointsModule;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.List;

/**
 * Geteilte Wegpunkte im Chat: die Rohzeile ("xaero-waypoint:Base:B:100:64:-200:...") wird zu
 * "Steve shared a waypoint: [Base 100, 64, -200]"; ein Klick auf den Teil in Klammern legt den
 * Wegpunkt an. Der Klick läuft über einen Client-Befehl, der nie an den Server geht.
 */
final class ChatWaypoints {
    private ChatWaypoints() {}

    static final String COMMAND = "aiowaypoint";

    static void register(AioCore core) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) ->
                dispatcher.register(ClientCommands.literal(COMMAND).then(
                        ClientCommands.argument("data", StringArgumentType.greedyString()).executes(c -> {
                            var f = SharedWaypoint.Found.fromPayload(StringArgumentType.getString(c, "data"));
                            WaypointsModule wp = core.module(WaypointsModule.class);
                            if (f != null && wp != null) wp.addShared(f);
                            return 1;
                        }))));
    }

    /** Schreibt die Nachricht um, wenn sie einen geteilten Wegpunkt enthält; sonst unverändert. */
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
