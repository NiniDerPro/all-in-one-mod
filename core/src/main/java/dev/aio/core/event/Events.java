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

    /** Tastatur-Event. {@code action}: 0 = losgelassen, 1 = gedrückt, 2 = wiederholt (GLFW). */
    public record Key(int key, int action, int modifiers, boolean screenOpen) {
        public boolean isPress() { return action == 1; }
        public boolean isRelease() { return action == 0; }
    }

    /** Maustasten-Event. {@code button}: 0 = links, 1 = rechts, 2 = Mitte (GLFW). */
    public record MouseButton(int button, int action, boolean screenOpen) {
        public boolean isPress() { return action == 1; }
    }

    /** Mausrad im Spiel (kein Screen offen). Abbrechen verhindert das Scrollen der Hotbar. */
    public static final class MouseScroll extends Cancellable {
        public final double amount;

        public MouseScroll(double amount) { this.amount = amount; }
    }

    /** Beim Betreten einer Welt bzw. eines Servers. {@code address} ist bei Singleplayer null. */
    public record ServerJoin(String address) {}

    public static final class ServerLeave {
        public static final ServerLeave INSTANCE = new ServerLeave();
        private ServerLeave() {}
    }
}
