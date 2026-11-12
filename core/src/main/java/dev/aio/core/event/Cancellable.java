package dev.aio.core.event;

/** Base for events that a listener can cancel, e.g. scrolling while zooming. */
public abstract class Cancellable {
    private boolean cancelled;

    public boolean isCancelled() { return cancelled; }

    public void cancel() { cancelled = true; }
}
