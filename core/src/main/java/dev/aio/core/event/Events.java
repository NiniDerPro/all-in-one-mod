package dev.aio.core.event;

import dev.aio.core.platform.Renderer;

/** All events that the adapter posts into the core. */
public final class Events {
    private Events() {}

    /** Once per client tick (20/s), after the vanilla tick. */
    public static final class Tick {
        public static final Tick INSTANCE = new Tick();
        private Tick() {}
    }

    /** Once per frame, when the HUD is drawn. */
    public static final class RenderHud {
        public final Renderer renderer;
        public final float tickDelta;

        public RenderHud(Renderer renderer, float tickDelta) {
            this.renderer = renderer;
            this.tickDelta = tickDelta;
        }
    }

    /** Keyboard event with MC key names. {@code action}: 0 = released, 1 = pressed, 2 = repeated. */
    public record Key(String key, int action, boolean screenOpen) {
        public boolean isPress() { return action == 1; }
        public boolean isRelease() { return action == 0; }
    }

    /** Maustasten-Event. {@code button}: 0 = links, 1 = rechts, 2 = Mitte. */
    public record MouseButton(int button, int action, boolean screenOpen) {
        public boolean isPress() { return action == 1; }
    }

    /** Mouse wheel in game (no screen open). Cancelling prevents the hotbar from scrolling. */
    public static final class MouseScroll extends Cancellable {
        public final double amount;

        public MouseScroll(double amount) { this.amount = amount; }
    }

    /** Shortly before the player starts breaking a block (before sending to the server). */
    public record StartBreak(int x, int y, int z) {}

    /** Shortly before the player hits an entity (before sending to the server). */
    public record AttackEntity(boolean living) {}

    /**
     * A received chat or system message as plain text.
     *
     * @param fromSelf sent by the own player (according to the sender, only known for signed chat messages)
     */
    public record ChatReceived(String text, boolean system, boolean fromSelf) {
        public ChatReceived(String text, boolean system) { this(text, system, false); }
    }

    /** When entering a world or server. {@code address} is null in singleplayer. */
    public record ServerJoin(String address) {}

    public static final class ServerLeave {
        public static final ServerLeave INSTANCE = new ServerLeave();
        private ServerLeave() {}
    }
}
