package dev.aio.core.modules.hud;

import dev.aio.core.hud.TextHudElement;

/** Latenz zum Server laut Spielerliste. */
public final class PingModule extends TextHudElement {
    public PingModule() {
        super("ping", "Ping", "Latency to the server", dev.aio.core.module.Category.QOL, false, 0.0, 0.18);
    }

    @Override protected String getText() {
        if (!platform().inWorld() || platform().isSingleplayer()) return "";
        return platform().ping() + " ms";
    }

    @Override protected String previewText() { return "42 ms"; }
}
