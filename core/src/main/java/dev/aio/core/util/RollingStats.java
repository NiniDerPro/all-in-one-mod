package dev.aio.core.util;

/** Ringpuffer für Durchschnitt, Minimum und Maximum über die letzten N Werte. */
public final class RollingStats {
    private int[] values;
    private int size;
    private int head;

    public RollingStats(int capacity) {
        values = new int[Math.max(1, capacity)];
    }

    public void resize(int capacity) {
        capacity = Math.max(1, capacity);
        if (capacity == values.length) return;
        values = new int[capacity];
        size = 0;
        head = 0;
    }

    public void add(int v) {
        values[head] = v;
        head = (head + 1) % values.length;
        if (size < values.length) size++;
    }

    public void clear() { size = 0; head = 0; }

    public int size() { return size; }

    public int average() {
        if (size == 0) return 0;
        long sum = 0;
        for (int i = 0; i < size; i++) sum += values[i];
        return (int) Math.round(sum / (double) size);
    }

    public int min() {
        if (size == 0) return 0;
        int m = Integer.MAX_VALUE;
        for (int i = 0; i < size; i++) m = Math.min(m, values[i]);
        return m;
    }

    public int max() {
        if (size == 0) return 0;
        int m = Integer.MIN_VALUE;
        for (int i = 0; i < size; i++) m = Math.max(m, values[i]);
        return m;
    }
}
