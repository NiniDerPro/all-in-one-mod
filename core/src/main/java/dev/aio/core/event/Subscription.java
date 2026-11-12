package dev.aio.core.event;

/** Handle of a bus registration. {@link #cancel()} detaches the listener again. */
@FunctionalInterface
public interface Subscription {
    void cancel();
}
