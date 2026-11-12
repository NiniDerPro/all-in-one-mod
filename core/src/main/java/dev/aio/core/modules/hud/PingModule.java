package dev.aio.core.modules.hud;

import dev.aio.core.hud.TextHudElement;
import dev.aio.core.module.Category;

/** Latency to the server according to the player list. */
public final class PingModule extends TextHudElement {
    public PingModule() {
        super("ping", "Ping", "Latency to the server", Category.QOL, false, 0.0, 0.18);
    }

    @Override protected String getText() {
        if (!platform().inWorld() || platform().isSingleplayer()) return "";
        return platform().ping() + " ms";
    }

    @Override protected String previewText() { return "42 ms"; }
}
