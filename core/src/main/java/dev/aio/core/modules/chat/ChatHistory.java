package dev.aio.core.modules.chat;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Letzte Chatnachrichten als reiner Text (für Chat kopieren). */
public final class ChatHistory {
    public record Entry(String time, String text) {}

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final int capacity;
    private final ArrayDeque<Entry> entries = new ArrayDeque<>();

    public ChatHistory(int capacity) { this.capacity = capacity; }

    public synchronized void add(String text) {
        if (text == null || text.isBlank()) return;
        entries.addLast(new Entry(LocalTime.now().format(FMT), text));
        while (entries.size() > capacity) entries.pollFirst();
    }

    public synchronized List<Entry> entries() { return new ArrayList<>(entries); }

    public synchronized void clear() { entries.clear(); }
}
