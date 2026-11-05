package dev.aio.core.modules.chat;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.setting.BooleanSetting;

/**
 * Chat Heads: der Kopf des Absenders vor Chatnachrichten. Der Absender ist ein Spieler aus der
 * Tab-Liste, dessen Name früh in der Nachricht steht ({@code <Name>}, {@code Name:} oder einfach als
 * Wort) – so klappt es auch mit Server-Chatformaten mit Rang davor.
 */
public final class ChatHeadsModule extends Module {
    private final BooleanSetting systemMessages;

    public ChatHeadsModule() {
        super("chatheads", "Chat Heads", "Shows the sender's head in front of chat messages", Category.QOL, false);
        systemMessages = add(new BooleanSetting("system", "Also for server messages", true));
        systemMessages.onChange(v -> platform().refreshChat());
    }

    /** Auch bei Nachrichten, die der Server schickt (Chat-Plugins senden Spielerchat oft so). */
    public boolean includeSystemMessages() { return systemMessages.on(); }

    @Override protected void onActivate() { platform().refreshChat(); }

    @Override protected void onDeactivate() { platform().refreshChat(); }

    /**
     * Absender in einer Nachricht: der Name, der am weitesten vorne als ganzes Wort vorkommt; nur in
     * den ersten 48 Zeichen, damit eine Erwähnung mitten im Satz nicht zählt.
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
