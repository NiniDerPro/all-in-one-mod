package dev.aio.core.modules.info;

import dev.aio.core.hud.HudElement;
import dev.aio.core.module.Category;
import dev.aio.core.platform.Renderer;
import dev.aio.core.platform.ServerInfo;
import dev.aio.core.setting.BooleanSetting;

import java.util.ArrayList;
import java.util.List;

/** Server-Infos: Name, IP, Ping, Spieler, Version, Software. */
public final class ServerInfoModule extends HudElement {
    private static final int LINE = 10;

    private final BooleanSetting showName, showIp, showPing, showPlayers, showVersion, showBrand;
    private List<String> lines = List.of();
    private int cachedW = -1;

    public ServerInfoModule() {
        super("serverinfo", "Server Info", "Server IP, ping, players and version", Category.INFO, false, 1.0, 0.3);
        showName = add(new BooleanSetting("name", "Server name", true));
        showIp = add(new BooleanSetting("ip", "IP", true));
        showPing = add(new BooleanSetting("ping", "Ping", true));
        showPlayers = add(new BooleanSetting("players", "Players online", true));
        showVersion = add(new BooleanSetting("version", "Version", false));
        showBrand = add(new BooleanSetting("brand", "Server software", false));
    }

    @Override protected void onActivate() { update(); }

    @Override public void update() {
        List<String> next = new ArrayList<>();
        if (platform().inWorld() && !platform().isSingleplayer()) {
            ServerInfo s = platform().serverInfo();
            if (s != null) {
                if (showName.on() && !s.name().isEmpty()) next.add(s.name());
                if (showIp.on() && !s.address().isEmpty()) next.add("IP: " + s.address());
                if (showPing.on()) next.add("Ping: " + s.ping() + " ms");
                if (showPlayers.on()) next.add("Players: " + s.players());
                if (showVersion.on() && !s.version().isEmpty()) next.add("Version: " + s.version());
                if (showBrand.on() && !s.brand().isEmpty()) next.add("Software: " + s.brand());
            }
        }
        if (!next.equals(lines)) {
            lines = next;
            cachedW = -1;
        }
    }

    @Override public boolean hasContent() { return !lines.isEmpty(); }

    private List<String> shown(boolean preview) {
        return lines.isEmpty() && preview ? List.of("My Server", "IP: play.example.net", "Ping: 42 ms", "Players: 17") : lines;
    }

    @Override public int width(Renderer r) {
        if (cachedW < 0 || lines.isEmpty()) {
            int w = 0;
            for (String l : shown(true)) w = Math.max(w, r.textWidth(l));
            cachedW = w + PADDING * 2;
        }
        return cachedW;
    }

    @Override public int height(Renderer r) { return PADDING * 2 + shown(true).size() * LINE - 2; }

    @Override protected void renderContent(Renderer r, boolean preview) {
        List<String> s = shown(preview);
        for (int i = 0; i < s.size(); i++) drawText(r, s.get(i), PADDING, PADDING + i * LINE);
    }
}
