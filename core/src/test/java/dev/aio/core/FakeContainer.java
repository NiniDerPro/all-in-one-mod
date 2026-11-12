package dev.aio.core;

import dev.aio.core.platform.ContainerView;
import dev.aio.core.platform.ItemRef;

import java.util.ArrayList;
import java.util.List;

/**
 * Simulated inventory with the click rules of Minecraft (left click takes/places/swaps/stacks,
 * right click places one or takes half, shift click moves into the other area).
 */
public final class FakeContainer implements ContainerView {
    public record Stack(String id, int count, int max) implements ItemRef {
        @Override public boolean isEmpty() { return count <= 0; }
        @Override public int damage() { return 0; }
        @Override public int maxDamage() { return 0; }
        @Override public String name() { return id; }
        @Override public int maxStackSize() { return max; }
        Stack withCount(int c) { return new Stack(id, c, max); }
    }

    public static final Stack EMPTY = new Stack("", 0, 64);

    private final Stack[] items;
    private final boolean[] player;
    private final int[] containerSlot;
    private final boolean playerOnly;
    private Stack cursor = EMPTY;
    public int clicks;

    /** @param chestSlots number of chest slots before the 36 player slots (0 = player inventory only) */
    public FakeContainer(int chestSlots) {
        int n = chestSlots + 36;
        items = new Stack[n];
        player = new boolean[n];
        containerSlot = new int[n];
        playerOnly = chestSlots == 0;
        for (int i = 0; i < n; i++) {
            items[i] = EMPTY;
            player[i] = i >= chestSlots;
            // like in MC: main inventory (9-35) before the hotbar (0-8)
            int p = i - chestSlots;
            containerSlot[i] = player[i] ? (p < 27 ? p + 9 : p - 27) : i;
        }
    }

    public void set(int index, String id, int count) { items[index] = new Stack(id, count, 64); }

    public void set(int index, String id, int count, int max) { items[index] = new Stack(id, count, max); }

    public Stack get(int index) { return items[index]; }

    public int total(String id) {
        int t = 0;
        for (Stack s : items) if (s.id.equals(id)) t += s.count;
        return t + (cursor.id.equals(id) ? cursor.count : 0);
    }

    @Override public List<Slot> slots() {
        List<Slot> out = new ArrayList<>();
        for (int i = 0; i < items.length; i++) out.add(new Slot(i, items[i], player[i], containerSlot[i]));
        return out;
    }

    @Override public ItemRef cursor() { return cursor; }

    @Override public boolean isPlayerOnly() { return playerOnly; }

    public Grid grid;

    @Override public Grid grid() { return grid; }

    /** Like vanilla quick craft (drag left): distribute evenly, respect the stack limit, rest on the cursor. */
    @Override public void dragSplit(int[] slots) {
        clicks += slots.length + 2;
        if (cursor.isEmpty() || slots.length == 0) return;
        if (slots.length == 1) { clicks -= 2; clicks--; click(slots[0], 0, Click.PICKUP); return; }
        int each = cursor.count / slots.length;
        int left = cursor.count;
        for (int i : slots) {
            Stack t = items[i];
            if (!t.isEmpty() && !t.id.equals(cursor.id)) continue;
            int have = t.isEmpty() ? 0 : t.count;
            int now = Math.min(cursor.max, have + each);
            left -= now - have;
            items[i] = cursor.withCount(now);
        }
        cursor = left > 0 ? cursor.withCount(left) : EMPTY;
    }

    @Override public void click(int i, int button, Click type) {
        clicks++;
        Stack slot = items[i];
        if (type == Click.QUICK_MOVE) {
            if (slot.isEmpty()) return;
            int remaining = slot.count;
            for (int j = 0; j < items.length && remaining > 0; j++) {
                if (j == i || player[j] == player[i] && !playerOnly) continue;
                if (playerOnly && (containerSlot[j] < 9) == (containerSlot[i] < 9)) continue;
                Stack t = items[j];
                if (t.isEmpty()) { items[j] = slot.withCount(remaining); remaining = 0; }
                else if (t.id.equals(slot.id) && t.count < t.max) {
                    int move = Math.min(remaining, t.max - t.count);
                    items[j] = t.withCount(t.count + move);
                    remaining -= move;
                }
            }
            items[i] = remaining > 0 ? slot.withCount(remaining) : EMPTY;
            return;
        }
        if (button == 0) {
            if (cursor.isEmpty()) { cursor = slot; items[i] = EMPTY; }
            else if (slot.isEmpty()) { items[i] = cursor; cursor = EMPTY; }
            else if (slot.id.equals(cursor.id)) {
                int move = Math.min(cursor.count, slot.max - slot.count);
                items[i] = slot.withCount(slot.count + move);
                cursor = cursor.count - move > 0 ? cursor.withCount(cursor.count - move) : EMPTY;
            } else { items[i] = cursor; cursor = slot; }
        } else {
            if (cursor.isEmpty()) {
                if (slot.isEmpty()) return;
                int half = (slot.count + 1) / 2;
                cursor = slot.withCount(half);
                items[i] = slot.count - half > 0 ? slot.withCount(slot.count - half) : EMPTY;
            } else if (slot.isEmpty() || slot.id.equals(cursor.id) && slot.count < slot.max) {
                items[i] = slot.isEmpty() ? cursor.withCount(1) : slot.withCount(slot.count + 1);
                cursor = cursor.count - 1 > 0 ? cursor.withCount(cursor.count - 1) : EMPTY;
            } else { items[i] = cursor; cursor = slot; }
        }
    }
}
