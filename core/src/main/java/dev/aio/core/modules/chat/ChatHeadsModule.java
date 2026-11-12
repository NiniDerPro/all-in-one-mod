package dev.aio.core.modules.chat;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.setting.BooleanSetting;

/**
 * Chat Heads: the sender's head in front of chat messages. The sender is a player from the
 * tab list whose name appears early in the message ({@code <Name>}, {@code Name:} or simply as a
 * word) – so it also works with server chat formats with a rank in front.
 */
public final class ChatHeadsModule extends Module {
    private final BooleanSetting systemMessages;

    public ChatHeadsModule() {
        super("chatheads", "Chat Heads", "Shows the sender's head in front of chat messages", Category.QOL, false);
        systemMessages = add(new BooleanSetting("system", "Also for server messages", true));
        systemMessages.onChange(v -> platform().refreshChat());
    }

    /** Also for messages sent by the server (chat plugins often send player chat that way). */
    public boolean includeSystemMessages() { return systemMessages.on(); }

    @Override protected void onActivate() { platform().refreshChat(); }

    @Override protected void onDeactivate() { platform().refreshChat(); }

    /**
     * Sender in a message: the name that appears furthest forward as a whole word; only in
     * the first 48 characters, so that a mention in the middle of a sentence does not count.
     */
    public static String findSender(String text, Iterable<String> names) {
        String best = null;
        int bestAt = Integer.MAX_VALUE;
        int limit = Math.min(text.length(), 48);
        for (String n : names) {
            if (n == null || n.length() < 3) continue;
            int at = text.indexOf(n);
            while (at >= 0 && at < limit) {
                boolean startOk = at == 0 || !isNameChar(text.charAt(at - 1));
                int end = at + n.length();
                boolean endOk = end >= text.length() || !isNameChar(text.charAt(end));
                if (startOk && endOk) break;
                at = text.indexOf(n, at + 1);
            }
            if (at >= 0 && at < limit && (at < bestAt || at == bestAt && n.length() > best.length())) {
                best = n;
                bestAt = at;
            }
        }
        return best;
    }

    private static boolean isNameChar(char c) { return Character.isLetterOrDigit(c) || c == '_'; }
}
