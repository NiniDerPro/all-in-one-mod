package dev.aio.fabric;

import dev.aio.core.AioCore;
import dev.aio.core.modules.chat.ChatHeadsModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.network.chat.Style;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Chat Heads: Absender je Nachricht finden, ihre Zeilen einrücken und beim Zeichnen den Kopf davor
 * setzen. Die Zeilen merken wir uns über ihren Inhalt (dasselbe Objekt kommt beim Zeichnen an).
 */
public final class ChatHeads {
    private ChatHeads() {}

    /** Platz vor dem Text: 8 px Kopf und Abstand (drei Leerzeichen à 4 px). */
    private static final String INDENT = "   ";

    private record Head(PlayerSkin skin, boolean firstLine) {}

    private static final Map<FormattedCharSequence, Head> LINES = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<GuiMessage, PlayerSkin> SENDERS = Collections.synchronizedMap(new WeakHashMap<>());
    private static GuiMessage lastMessage;

    private static ChatHeadsModule module() {
        AioCore core = AioCore.get();
        ChatHeadsModule m = core == null ? null : core.module(ChatHeadsModule.class);
        return m != null && m.isActive() ? m : null;
    }

    /** Beim Anlegen einer Chatzeile: ggf. einrücken und den Kopf vormerken. */
    public static FormattedCharSequence line(GuiMessage message, FormattedCharSequence content) {
        ChatHeadsModule m = module();
        if (m == null) return content;
        PlayerSkin skin = SENDERS.computeIfAbsent(message, ChatHeads::sender);
        if (skin == null) return content;
        boolean first = message != lastMessage;
        lastMessage = message;
        FormattedCharSequence indented = FormattedCharSequence.composite(FormattedCharSequence.forward(INDENT, Style.EMPTY), content);
        LINES.put(indented, new Head(skin, first));
        return indented;
    }

    private static PlayerSkin sender(GuiMessage message) {
        ChatHeadsModule m = module();
        var connection = Minecraft.getInstance().getConnection();
        if (m == null || connection == null) return null;
        if (message.source() != GuiMessageSource.PLAYER && !m.includeSystemMessages()) return null;
        if (message.source() == GuiMessageSource.SYSTEM_CLIENT) return null;
        List<String> names = new ArrayList<>();
        for (PlayerInfo info : connection.getOnlinePlayers()) names.add(info.getProfile().name());
        String name = ChatHeadsModule.findSender(message.content().getString(), names);
        PlayerInfo info = name == null ? null : connection.getPlayerInfo(name);
        return info == null ? null : info.getSkin();
    }

    /** Beim Zeichnen einer Zeile: Kopf an den Zeilenanfang (in der Chat-Pose, gleiche Deckkraft wie der Text). */
    public static void draw(GuiGraphicsExtractor graphics, int y, float alpha, FormattedCharSequence content) {
        Head head = LINES.get(content);
        if (head == null || !head.firstLine() || module() == null) return;
        PlayerFaceExtractor.extractRenderState(graphics, head.skin(), 0, y, 8, ARGB.white(alpha));
    }

    /** Chatzeilen neu aufbauen (Ein-/Ausschalten). */
    static void refresh() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui == null || mc.gui.hud == null) return;
        SENDERS.clear();
        LINES.clear();
        lastMessage = null;
        mc.gui.hud.getChat().rescaleChat();
    }
}
