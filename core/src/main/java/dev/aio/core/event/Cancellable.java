package dev.aio.core.event;

/** Basis für Events, die ein Listener abbrechen kann, z. B. Scrollen während Zoom. */
public abstract class Cancellable {
    private boolean cancelled;

    public boolean isCancelled() { return cancelled; }

    public void cancel() { cancelled = true; }
}
