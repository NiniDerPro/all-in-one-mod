package dev.aio.core.event;

import dev.aio.core.platform.Renderer;

/** Alle Events, die der Adapter in den Kern postet. */
public final class Events {
    private Events() {}

    /** Einmal pro Client-Tick (20/s), nach dem Vanilla-Tick. */
    public static final class Tick {
        public static final Tick INSTANCE = new Tick();
        private Tick() {}
    }

    /** Einmal pro Frame, wenn das HUD gezeichnet wird. */
    public static final class RenderHud {
        public final Renderer renderer;
        public final float tickDelta;

        public RenderHud(Renderer renderer, float tickDelta) {
            this.renderer = renderer;
            this.tickDelta = tickDelta;
        }
    }

    /** Tastatur-Event mit MC-Tastennamen. {@code action}: 0 = losgelassen, 1 = gedrückt, 2 = wiederholt. */
    public record Key(String key, int action, boolean screenOpen) {
        public boolean isPress() { return action == 1; }
        public boolean isRelease() { return action == 0; }
    }

    /** Maustasten-Event. {@code button}: 0 = links, 1 = rechts, 2 = Mitte. */
    public record MouseButton(int button, int action, boolean screenOpen) {
        public boolean isPress() { return action == 1; }
    }

    /** Mausrad im Spiel (kein Screen offen). Abbrechen verhindert das Scrollen der Hotbar. */
    public static final class MouseScroll extends Cancellable {
        public final double amount;

        public MouseScroll(double amount) { this.amount = amount; }
    }

    /** Kurz bevor der Spieler anfängt, einen Block abzubauen (vor dem Senden an den Server). */
    public record StartBreak(int x, int y, int z) {}

    /** Kurz bevor der Spieler ein Wesen schlägt (vor dem Senden an den Server). */
    public record AttackEntity(boolean living) {}

    /**
     * Eine empfangene Chat- oder Systemnachricht als reiner Text.
     *
     * @param fromSelf vom eigenen Spieler gesendet (laut Absender, nur bei signierten Chatnachrichten bekannt)
     */
    public record ChatReceived(String text, boolean system, boolean fromSelf) {
        public ChatReceived(String text, boolean system) { this(text, system, false); }
    }

    /** Beim Betreten einer Welt bzw. eines Servers. {@code address} ist bei Singleplayer null. */
    public record ServerJoin(String address) {}

    public static final class ServerLeave {
        public static final ServerLeave INSTANCE = new ServerLeave();
        private ServerLeave() {}
    }
}
