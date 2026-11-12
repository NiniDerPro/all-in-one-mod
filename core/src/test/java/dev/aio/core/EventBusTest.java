package dev.aio.core;

import dev.aio.core.event.EventBus;
import dev.aio.core.event.Subscription;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EventBusTest {
    record Ping(int n) {}
    record Other() {}

    @Test void dispatchesByExactTypeAndPriority() {
        EventBus bus = new EventBus();
        List<String> order = new ArrayList<>();
        bus.subscribe(Ping.class, p -> order.add("normal1"));
        bus.subscribe(Ping.class, p -> order.add("high"), EventBus.PRIORITY_HIGH);
        bus.subscribe(Ping.class, p -> order.add("low"), EventBus.PRIORITY_LOW);
        bus.subscribe(Ping.class, p -> order.add("normal2"));
        bus.subscribe(Other.class, o -> order.add("other"));
        bus.post(new Ping(1));
        assertEquals(List.of("high", "normal1", "normal2", "low"), order);
    }

    @Test void unsubscribeRemovesListenerAndEmptyEntry() {
        EventBus bus = new EventBus();
        int[] count = {0};
        Subscription a = bus.subscribe(Ping.class, p -> count[0]++);
        Subscription b = bus.subscribe(Ping.class, p -> count[0] += 10);
        bus.post(new Ping(0));
        a.cancel();
        bus.post(new Ping(0));
        assertEquals(21, count[0]);
        b.cancel();
        b.cancel(); // twice is harmless
        assertFalse(bus.hasListeners(Ping.class));
    }

    @Test void failingListenerDoesNotStopOthers() {
        EventBus bus = new EventBus();
        int[] count = {0};
        bus.subscribe(Ping.class, p -> { throw new RuntimeException("boom"); });
        bus.subscribe(Ping.class, p -> count[0]++);
        bus.post(new Ping(0));
        assertEquals(1, count[0]);
    }

    @Test void unsubscribeDuringDispatchIsSafe() {
        EventBus bus = new EventBus();
        int[] count = {0};
        Subscription[] self = new Subscription[1];
        self[0] = bus.subscribe(Ping.class, p -> { count[0]++; self[0].cancel(); });
        bus.subscribe(Ping.class, p -> count[0]++);
        bus.post(new Ping(0));
        bus.post(new Ping(0));
        assertEquals(3, count[0]);
    }
}
