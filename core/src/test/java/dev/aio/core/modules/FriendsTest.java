package dev.aio.core.modules;

import dev.aio.core.AioCore;
import dev.aio.core.FakePlatform;
import dev.aio.core.modules.render.FriendsModule;
import dev.aio.core.modules.render.FriendsModule.Match;
import dev.aio.core.modules.render.FriendsModule.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FriendsTest {
    @TempDir Path dir;

    private FriendsModule start() {
        AioCore core = new AioCore(new FakePlatform(), dir);
        core.registerDefaultModules();
        core.start();
        FriendsModule m = core.module(FriendsModule.class);
        m.setEnabled(true);
        return m;
    }

    @Test void rulesMatchByTypeAndFirstRuleWins() {
        FriendsModule m = start();
        m.addRule(new Rule(Match.NAME, "Alex", FriendsModule.FRIEND_GREEN, true));
        m.addRule(new Rule(Match.CONTAINS, "{abc}", 0xFF5599FF, true));
        m.addRule(new Rule(Match.STARTS, "mod_", 0xFFFF5555, true));
        m.addRule(new Rule(Match.ENDS, "_x", 0xFFFFAA00, true));

        assertEquals(FriendsModule.FRIEND_GREEN, m.colorFor("alex", null, null));
        assertEquals(FriendsModule.NONE, m.colorFor("Alexander", null, null), "name rules match the whole name only");
        // tag only in the shown name (team prefix with colour codes)
        assertEquals(0xFF5599FF, m.colorFor("Steve", "§c{ABC} §fSteve", null));
        assertEquals(0xFF5599FF, m.colorFor("Steve", null, "{abc} Steve"));
        assertEquals(0xFFFF5555, m.colorFor("Mod_Kim", null, null));
        assertEquals(0xFFFFAA00, m.colorFor("kim_x", null, null));
        // first rule wins
        assertEquals(FriendsModule.FRIEND_GREEN, m.colorFor("Alex", "{abc} Alex", null));
        assertEquals(FriendsModule.NONE, m.colorFor("Nobody", "Nobody", "Nobody"));
    }

    @Test void disabledRulesAndModuleOff() {
        FriendsModule m = start();
        m.addRule(new Rule(Match.NAME, "Alex", FriendsModule.FRIEND_GREEN, true));
        Rule r = m.rules().get(0);
        m.replace(r, r.withEnabled(false));
        assertEquals(FriendsModule.NONE, m.colorFor("Alex", null, null));
        m.replace(m.rules().get(0), m.rules().get(0).withEnabled(true));
        assertEquals(FriendsModule.FRIEND_GREEN, m.colorFor("Alex", null, null));
        m.setEnabled(false);
        assertEquals(FriendsModule.NONE, m.colorFor("Alex", null, null));
    }

    @Test void duplicatesAreSkippedAndRulesPersist() {
        FriendsModule m = start();
        m.addRule(new Rule(Match.NAME, "Alex", FriendsModule.FRIEND_GREEN, true));
        m.addRule(new Rule(Match.NAME, "ALEX", 0xFFFF5555, true));
        m.addRule(new Rule(Match.CONTAINS, "  ", 0xFFFF5555, true));
        assertEquals(1, m.rules().size());
        m.addRule(new Rule(Match.CONTAINS, "tag", 0xFF5599FF, false));

        FriendsModule again = start();
        assertEquals(m.rules(), again.rules());
        m.remove(m.rules().get(0));
        assertEquals(1, start().rules().size());
    }

    @Test void tintMixesWithWhite() {
        FriendsModule m = start();
        int t = m.tintFor(0xFF00FF00);
        assertEquals(0xFF, (t >> 8) & 0xFF);
        assertTrue(((t >> 16) & 0xFF) < 255 && ((t >> 16) & 0xFF) > 0);
    }
}
