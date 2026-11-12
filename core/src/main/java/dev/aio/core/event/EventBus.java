package dev.aio.core.event;

import dev.aio.core.util.Log;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Schlanker, typisierter Event-Bus.
 *
 * <ul>
 *   <li>Dispatch only via the exact event class (no hierarchy search, no reflection scan).</li>
 *   <li>Copy-on-write arrays: {@link #post} iterates without locks and without allocation.</li>
 *   <li>Without listeners a {@link #post} costs only a map lookup. That is why
 *       disabled modules detach their listeners completely.</li>
 *   <li>An exception in one listener is logged and does not abort the others.</li>
 * </ul>
 */
public final class EventBus {
    /** Higher priority runs first. */
    public static final int PRIORITY_HIGH = 100;
    public static final int PRIORITY_NORMAL = 0;
    public static final int PRIORITY_LOW = -100;

    private static final Listener<?>[] EMPTY = new Listener<?>[0];

    private record Listener<E>(Consumer<E> handler, int priority, long order) {}

    private final Map<Class<?>, Listener<?>[]> listeners = new ConcurrentHashMap<>();
    private long counter;

    public <E> Subscription subscribe(Class<E> type, Consumer<E> handler) {
        return subscribe(type, handler, PRIORITY_NORMAL);
    }

    public synchronized <E> Subscription subscribe(Class<E> type, Consumer<E> handler, int priority) {
        Listener<E> listener = new Listener<>(handler, priority, counter++);
        Listener<?>[] current = listeners.getOrDefault(type, EMPTY);
        Listener<?>[] next = Arrays.copyOf(current, current.length + 1);
        next[current.length] = listener;
        Arrays.sort(next, (a, b) -> a.priority != b.priority
                ? Integer.compare(b.priority, a.priority)
                : Long.compare(a.order, b.order));
        listeners.put(type, next);
        return () -> unsubscribe(type, listener);
    }

    private synchronized void unsubscribe(Class<?> type, Listener<?> listener) {
        Listener<?>[] current = listeners.get(type);
        if (current == null) return;
        int idx = -1;
        for (int i = 0; i < current.length; i++) {
            if (current[i] == listener) { idx = i; break; }
        }
        if (idx < 0) return;
        if (current.length == 1) {
            listeners.remove(type);
            return;
        }
        Listener<?>[] next = new Listener<?>[current.length - 1];
        System.arraycopy(current, 0, next, 0, idx);
        System.arraycopy(current, idx + 1, next, idx, current.length - idx - 1);
        listeners.put(type, next);
    }

    @SuppressWarnings("unchecked")
    public <E> E post(E event) {
        Listener<?>[] arr = listeners.get(event.getClass());
        if (arr == null) return event;
        for (Listener<?> l : arr) {
            try {
                ((Consumer<E>) l.handler).accept(event);
            } catch (Throwable t) {
                Log.error("Listener for " + event.getClass().getSimpleName() + " failed", t);
            }
        }
        return event;
    }

    public boolean hasListeners(Class<?> type) {
        return listeners.containsKey(type);
    }

    public int listenerCount(Class<?> type) {
        Listener<?>[] arr = listeners.get(type);
        return arr == null ? 0 : arr.length;
    }
}
