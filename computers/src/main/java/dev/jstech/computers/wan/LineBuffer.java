/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.wan;

import dev.jstech.computers.storage.IDataSink;
import dev.jstech.computers.storage.StorageKey;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The items on a long-distance line: what the far network has sent and the near one has not taken in yet. It takes in
 * no more than its line holds, so the far side sends at the line's speed and not at its own.
 */
public final class LineBuffer implements IDataSink {

    private final Map<StorageKey, Long> held = new LinkedHashMap<>();
    private long room;

    /** The line now holds at most {@code most} items. */
    public void holdAtMost(final long most) {
        room = Math.max(0L, most);
    }

    @Override
    public long insert(final StorageKey key, final long amount, final boolean simulate) {
        final long taken = Math.max(0L, Math.min(amount, room - count()));
        if (!simulate && taken > 0L) {
            held.merge(key, taken, Long::sum);
        }
        return taken;
    }

    /** What is on the line. */
    public Map<StorageKey, Long> held() {
        return Map.copyOf(held);
    }

    /** How many items are on the line. */
    public long count() {
        long total = 0L;
        for (final long amount : held.values()) {
            total += amount;
        }
        return total;
    }

    /** Hands at most {@code most} of what is on the line to {@code to}, and answers how many it took. */
    public long deliver(final IDataSink to, final long most) {
        long left = most;
        for (final Map.Entry<StorageKey, Long> entry : held.entrySet()) {
            if (left <= 0L) {
                break;
            }
            final long taken = to.insert(entry.getKey(), Math.min(left, entry.getValue()), false);
            entry.setValue(entry.getValue() - taken);
            left -= taken;
        }
        held.values().removeIf(amount -> amount <= 0L);
        return most - left;
    }

    /** Empties the line and answers what was on it. */
    public Map<StorageKey, Long> empty() {
        final Map<StorageKey, Long> was = Map.copyOf(held);
        held.clear();
        return was;
    }

    @Override
    public long dropHeld() {
        final long was = count();
        held.clear();
        return was;
    }
}
