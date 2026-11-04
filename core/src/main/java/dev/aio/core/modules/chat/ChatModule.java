package dev.aio.core.modules.chat;

import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.NumberSetting;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Chat-Verbesserungen: Zeitstempel und längerer Verlauf. */
public final class ChatModule extends Module {
    public static final int VANILLA_HISTORY = 100;
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter HMS = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final BooleanSetting timestamps;
    private final BooleanSetting seconds;
    private final NumberSetting history;

    public ChatModule() {
        super("chat", "Chat", "Zeitstempel und längerer Chatverlauf", Category.CHAT, false);
        timestamps = add(new BooleanSetting("timestamps", "Zeitstempel", true));
        seconds = add(new BooleanSetting("seconds", "Mit Sekunden", false));
        seconds.visibleWhen(timestamps::on);
        history = add(new NumberSetting("history", "Verlaufslänge", 500, 100, 2000, 50, " Zeilen"));
    }

    /** Präfix wie {@code [12:34] } oder null, wenn deaktiviert. */
    public String timestampPrefix() {
        if (!isActive() || !timestamps.on()) return null;
        return "[" + LocalTime.now().format(seconds.on() ? HMS : HM) + "] ";
    }

    public int historySize() {
        return isActive() ? history.intValue() : VANILLA_HISTORY;
    }
}
