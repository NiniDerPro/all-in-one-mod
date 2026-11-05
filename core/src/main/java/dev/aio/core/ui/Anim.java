package dev.aio.core.ui;

import java.util.HashMap;
import java.util.Map;

/** Weiche Übergänge: Werte gleiten zeitbasiert zu ihrem Ziel (unabhängig von den FPS). */
public final class Anim {
    private final Map<Object, Float> values = new HashMap<>();
    private long last = System.nanoTime();
    private float dt;

    /** Einmal pro Frame aufrufen. */
    public void frame() {
        long now = System.nanoTime();
        dt = Math.min(0.1f, (now - last) / 1e9f);
        last = now;
    }

    /** Nähert den Wert unter {@code key} dem Ziel an; {@code speed} ≈ 1/Sekunden bis fast am Ziel. */
    public float to(Object key, float target, float speed) {
        Float v = values.get(key);
        if (v == null) {
            values.put(key, target);
            return target;
        }
        float k = 1 - (float) Math.exp(-speed * dt);
        float next = v + (target - v) * k;
        if (Math.abs(target - next) < 0.001f) next = target;
        values.put(key, next);
        return next;
    }

    public void set(Object key, float value) { values.put(key, value); }

    public void clear() { values.clear(); }
}
