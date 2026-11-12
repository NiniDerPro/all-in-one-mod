package dev.aio.core.update;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class UpdateCheckTest {
    private static final String JSON = "[" +
            "{\"tag_name\":\"v0.4.3-mc26.3\",\"html_url\":\"https://x/a\",\"draft\":false}," +
            "{\"tag_name\":\"v0.4.10-mc26.3\",\"html_url\":\"https://x/b\",\"draft\":false}," +
            "{\"tag_name\":\"v0.9.0-mc26.3\",\"html_url\":\"https://x/draft\",\"draft\":true}," +
            "{\"tag_name\":\"v0.5.0-mc1.21.11\",\"html_url\":\"https://x/other\",\"draft\":false}," +
            "{\"tag_name\":\"v0.4.2-mc26.3\",\"html_url\":\"https://x/same\",\"draft\":false}]";

    @Test void picksNewestForSameMinecraft() {
        var r = UpdateCheck.pick(JSON, "0.4.2+mc26.3");
        assertNotNull(r);
        assertEquals("0.4.10", r.version());
        assertEquals("https://x/b", r.url());
    }

    @Test void noNewerMeansNull() {
        assertNull(UpdateCheck.pick(JSON, "0.4.10+mc26.3"));
        assertNull(UpdateCheck.pick(JSON, "0.4.2+mc1.20.1"));
        assertNull(UpdateCheck.pick(JSON, "garbage"));
        assertNull(UpdateCheck.pick("{}", "0.4.2+mc26.3"));
    }

    @Test void numericCompare() {
        assertTrue(UpdateCheck.compare("0.4.10", "0.4.2") > 0);
        assertEquals(0, UpdateCheck.compare("1.0", "1.0.0"));
    }
}
