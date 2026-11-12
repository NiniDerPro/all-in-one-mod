package dev.aio.core.modules.chat;

import dev.aio.core.event.Events;
import dev.aio.core.i18n.Lang;
import dev.aio.core.module.Category;
import dev.aio.core.module.Module;
import dev.aio.core.setting.ActionSetting;
import dev.aio.core.setting.BooleanSetting;
import dev.aio.core.setting.StringSetting;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Notifies when the own name (or a keyword) is mentioned in chat. */
public final class NameAlertModule extends Module {
    private final BooleanSetting requireAt;
    private final StringSetting keywords;
    private final BooleanSetting sound;
    private final BooleanSetting popup;
    private final BooleanSetting ignoreOwn;

    public NameAlertModule() {
        super("namealert", "Name Alert", "Sound and notice when someone mentions your name", Category.QOL, false);
        requireAt = add(new BooleanSetting("requireAt", "Only with @", false));
        keywords = add(new StringSetting("keywords", "Extra words (comma separated)", "", 256));
        sound = add(new BooleanSetting("sound", "Sound", true));
        popup = add(new BooleanSetting("popup", "Show notice", true));
        ignoreOwn = add(new BooleanSetting("ignoreOwn", "Ignore your own messages", true));
        add(new ActionSetting("test", "Test alert", () -> {
            if (sound.on()) platform().playAlertSound();
            platform().notify(Lang.tr("aio.ui.name_alert.mentioned_steve_hey", "Mentioned: <Steve> hey %s!", platform().playerName()));
        }));
        on(Events.ChatReceived.class, this::onChat);
    }


    private void onChat(Events.ChatReceived e) {
        String name = platform().playerName();
        if (ignoreOwn.on() && (e.fromSelf() || isOwnMessage(e.text(), name))) return;
        String hit = match(e.text(), words(name), requireAt.on());
        if (hit == null) return;
        if (sound.on()) platform().playAlertSound();
        if (popup.on()) platform().notify(Lang.tr("aio.ui.name_alert.mentioned", "Mentioned: %s", shorten(e.text())));
    }

    private List<String> words(String name) {
        List<String> list = new ArrayList<>();
        if (name != null && !name.isBlank()) list.add(name);
        for (String k : keywords.get().split(",")) if (!k.isBlank()) list.add(k.trim());
        return list;
    }

    /**
     * Recognises own messages with server chat formats too: at the start rank prefixes in
     * brackets may appear ("[VIP] ", "[Admin] [Lv 5] "), then comes the own name, followed by
     * a separator (":", ">", "»", "|", "→") or a space (join/system messages via
     * one itself, e.g. "Steve joined the game").
     */
    static boolean isOwnMessage(String text, String name) {
        if (name == null || name.isEmpty()) return false;
        String n = Pattern.quote(name);
        String regex = "^\\s*(?:[\\[(][^\\])]{0,24}[\\])]\\s*)*<?" + n + ">?(?:\\s*[:>»|→]|\\s|$)";
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text.replaceAll("§.", "")).find();
    }

    /** Returns the found word or null. Whole words, case-insensitive. */
    static String match(String text, List<String> words, boolean requireAt) {
        for (String w : words) {
            String q = Pattern.quote(w.toLowerCase(Locale.ROOT));
            // whole word, also with ending "s" or "'s" ("Steves", "Steve's")
            String end = "(?:'?s)?(?![\\w])";
            String regex = requireAt ? "@" + q + end : "(?<![\\w])@?" + q + end;
            if (Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text).find()) return w;
        }
        return null;
    }

    private static String shorten(String s) { return s.length() > 60 ? s.substring(0, 57) + "..." : s; }
}
