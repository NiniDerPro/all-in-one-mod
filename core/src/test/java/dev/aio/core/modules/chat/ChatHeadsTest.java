package dev.aio.core.modules.chat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChatHeadsTest {
    private static final List<String> ONLINE = List.of("Steve", "Alex", "Notch", "Steve2");

    @Test void findsTheSenderInCommonFormats() {
        assertEquals("Steve", ChatHeadsModule.findSender("<Steve> hi", ONLINE));
        assertEquals("Alex", ChatHeadsModule.findSender("[VIP] Alex: who has iron?", ONLINE));
        assertEquals("Steve2", ChatHeadsModule.findSender("Steve2 » hello", ONLINE), "ganzes Wort, nicht Steve");
        assertEquals("Notch", ChatHeadsModule.findSender("[12:00] <Notch> Steve, come here", ONLINE), "the foremost name counts");
    }

    @Test void noSenderForPlainServerText() {
        assertNull(ChatHeadsModule.findSender("Welcome to the server!", ONLINE));
        assertNull(ChatHeadsModule.findSender("x".repeat(60) + " Steve", ONLINE), "mention far back does not count");
    }
}
