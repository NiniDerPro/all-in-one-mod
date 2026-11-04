package dev.aio.core.event;

/** Handle einer Bus-Registrierung. {@link #cancel()} hängt den Listener wieder aus. */
@FunctionalInterface
public interface Subscription {
    void cancel();
}
