package dev.aio.core.modules.chat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NameAlertMatchTest {
    @Test void matchesNameAsWordWithPossessiveAndPlural() {
        List<String> w = List.of("Steve");
        assertEquals("Steve", NameAlertModule.match("hey steve, come here", w, false));
        assertEquals("Steve", NameAlertModule.match("that's Steve's house", w, false));
        assertEquals("Steve", NameAlertModule.match("all the Steves", w, false));
        assertEquals("Steve", NameAlertModule.match("@Steve look", w, false));
        assertNull(NameAlertModule.match("Stevenson is here", w, false));
        assertNull(NameAlertModule.match("steve here", w, true), "nur mit @");
    }

    @Test void ownMessagesAreRecognized() {
        assertTrue(NameAlertModule.isOwnMessage("<Steve> hi", "Steve"));
        assertTrue(NameAlertModule.isOwnMessage("[VIP] Steve: hi", "Steve"));
        assertFalse(NameAlertModule.isOwnMessage("<Alex> hi Steve", "Steve"));
    }
}
