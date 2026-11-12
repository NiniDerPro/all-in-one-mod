package dev.aio.core.util;

import dev.aio.core.event.EventBus;
import dev.aio.core.event.Events;
import dev.aio.core.event.Subscription;

import java.util.ArrayDeque;
import java.util.function.LongSupplier;

/**
 * Counts clicks per second, separate for left and right: timestamps in a queue,
 * everything older than 1 s drops out.
 *
 * <p>Shared by several modules (CPS, keystrokes). The mouse listener is only attached
 * while at least one module uses the tracker via {@link #acquire()}.
 */
public final class ClickTracker {
    public static final long WINDOW_NANOS = 1_000_000_000L;

    private final EventBus bus;
    private final LongSupplier clock;
    private final ArrayDeque<Long> left = new ArrayDeque<>();
    private final ArrayDeque<Long> right = new ArrayDeque<>();
    private int users;
    private Subscription subscription;

    public ClickTracker(EventBus bus, LongSupplier nanoClock) {
        this.bus = bus;
        this.clock = nanoClock;
    }

    public synchronized void acquire() {
        if (users++ == 0) {
            subscription = bus.subscribe(Events.MouseButton.class, e -> {
                if (e.isPress() && !e.screenOpen()) click(e.button());
            });
        }
    }

    public synchronized void release() {
        if (users == 0) return;
        if (--users == 0) {
            subscription.cancel();
            subscription = null;
            left.clear();
            right.clear();
        }
    }

    public boolean isListening() { return subscription != null; }

    public synchronized void click(int button) {
        long now = clock.getAsLong();
        if (button == 0) left.addLast(now);
        else if (button == 1) right.addLast(now);
    }

    public synchronized int leftCps() { return prune(left); }

    public synchronized int rightCps() { return prune(right); }

    private int prune(ArrayDeque<Long> queue) {
        long cutoff = clock.getAsLong() - WINDOW_NANOS;
        while (!queue.isEmpty() && queue.peekFirst() <= cutoff) queue.pollFirst();
        return queue.size();
    }
}
