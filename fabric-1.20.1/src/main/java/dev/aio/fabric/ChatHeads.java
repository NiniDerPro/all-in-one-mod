package dev.aio.fabric;

import dev.aio.core.AioCore;
import dev.aio.core.modules.chat.ChatHeadsModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.GuiMessage;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Chat Heads: find the sender per message, indent their lines and draw the head in front when drawing.
 * We remember the lines by their content (the same object arrives when drawing).
 */
public final class ChatHeads {
    private ChatHeads() {}

    /** Room in front of the text: 8 px head and gap (three spaces of 4 px). */
    private static final String INDENT = "   ";

    private record Head(ResourceLocation skin, boolean firstLine) {}

    private static final Map<FormattedCharSequence, Head> LINES = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<GuiMessage, ResourceLocation> SENDERS = Collections.synchronizedMap(new WeakHashMap<>());
    private static GuiMessage lastMessage;

    private static ChatHeadsModule module() {
        AioCore core = AioCore.get();
        ChatHeadsModule m = core == null ? null : core.module(ChatHeadsModule.class);
        return m != null && m.isActive() ? m : null;
    }

    /** When a chat line is created: indent if needed and note the head. */
    public static FormattedCharSequence line(GuiMessage message, FormattedCharSequence content) {
        ChatHeadsModule m = module();
        if (m == null) return content;
        ResourceLocation skin = SENDERS.computeIfAbsent(message, ChatHeads::sender);
        if (skin == null) return content;
        boolean first = message != lastMessage;
        lastMessage = message;
        FormattedCharSequence indented = FormattedCharSequence.composite(FormattedCharSequence.forward(INDENT, Style.EMPTY), content);
        LINES.put(indented, new Head(skin, first));
        return indented;
    }

    private static ResourceLocation sender(GuiMessage message) {
        ChatHeadsModule m = module();
        var connection = Minecraft.getInstance().getConnection();
        if (m == null || connection == null) return null;
        // Signed messages come from players; this version has no explicit message source
        if (message.signature() == null && !m.includeSystemMessages()) return null;
        List<String> names = new ArrayList<>();
        for (PlayerInfo info : connection.getOnlinePlayers()) names.add(info.getProfile().getName());
        String name = ChatHeadsModule.findSender(message.content().getString(), names);
        PlayerInfo info = name == null ? null : connection.getPlayerInfo(name);
        return info == null ? null : info.getSkinLocation();
    }

    /** When drawing a line: head at the line start (in the chat pose, same opacity as the text). */
    public static void draw(GuiGraphics graphics, int y, float alpha, FormattedCharSequence content) {
        Head head = LINES.get(content);
        if (head == null || !head.firstLine() || module() == null) return;
        graphics.setColor(1f, 1f, 1f, alpha);
        PlayerFaceRenderer.draw(graphics, head.skin(), 0, y, 8);
        graphics.setColor(1f, 1f, 1f, 1f);
    }

    /** Rebuild chat lines (switching on/off). */
    static void refresh() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui == null) return;
        SENDERS.clear();
        LINES.clear();
        lastMessage = null;
        mc.gui.getChat().rescaleChat();
    }
}
